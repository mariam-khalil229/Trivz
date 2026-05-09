package com.example.trivzserver.service;

import com.example.trivzserver.dto.GameQuestionResponse;
import com.example.trivzserver.entity.Player;
import com.example.trivzserver.entity.Question;
import com.example.trivzserver.entity.Room;
import com.example.trivzserver.entity.RoomGame;
import com.example.trivzserver.entity.RoomGameQuestion;
import com.example.trivzserver.repository.PlayerRepository;
import com.example.trivzserver.repository.QuestionRepository;
import com.example.trivzserver.repository.RoomGameQuestionRepository;
import com.example.trivzserver.repository.RoomGameRepository;
import com.example.trivzserver.repository.RoomRepository;
import com.example.trivzserver.repository.ScoreRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    public static final int ROUND_DURATION_MINUTES = 3;
    public static final int DEFAULT_QUESTION_COUNT = 10;
    public static final int QUESTION_TIME_LIMIT_SECONDS = 15;

    private final RoomRepository roomRepository;
    private final QuestionRepository questionRepository;
    private final RoomGameRepository roomGameRepository;
    private final RoomGameQuestionRepository roomGameQuestionRepository;
    private final PlayerRepository playerRepository;
    private final ScoreRepository scoreRepository;
    private final GameEventPublisher publisher;

    /**
     * Per-room set of question ids that have already been shown in this room across
     * any number of rounds. Lives only in memory — survives Play Again restarts but
     * not server restarts. The scope is small (a couple hundred ids per active room),
     * so a Map of sets is plenty.
     */
    private final Map<Long, Set<Long>> usedQuestionsByRoom = new ConcurrentHashMap<>();

    public GameService(RoomRepository roomRepository,
                       QuestionRepository questionRepository,
                       RoomGameRepository roomGameRepository,
                       RoomGameQuestionRepository roomGameQuestionRepository,
                       PlayerRepository playerRepository,
                       ScoreRepository scoreRepository,
                       GameEventPublisher publisher) {
        this.roomRepository = roomRepository;
        this.questionRepository = questionRepository;
        this.roomGameRepository = roomGameRepository;
        this.roomGameQuestionRepository = roomGameQuestionRepository;
        this.playerRepository = playerRepository;
        this.scoreRepository = scoreRepository;
        this.publisher = publisher;
    }

    @Transactional
    public GameQuestionResponse startGame(Long roomId, Integer questionCount) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        Player current = currentPlayer();
        if (room.getHostPlayerId() == null || !room.getHostPlayerId().equals(current.getId())) {
            throw new RuntimeException("Only the room host can start the game");
        }

        String status = room.getStatus();
        boolean restarting = "FINISHED".equalsIgnoreCase(status);
        if (!"LOBBY".equalsIgnoreCase(status) && !restarting) {
            throw new RuntimeException("Game is already in progress");
        }

        // Each round starts at zero — no cumulative scoring across rounds.
        if (restarting) {
            scoreRepository.deleteByRoomId(roomId);
            scoreRepository.flush();
        }

        int count = (questionCount == null || questionCount <= 0) ? DEFAULT_QUESTION_COUNT : questionCount;

        List<Question> selected = pickQuestions(room, count);
        if (selected.isEmpty()) {
            String diff = room.getDifficulty();
            String suffix = (diff != null && !diff.isBlank()) ? " for difficulty '" + diff + "'" : "";
            throw new RuntimeException("No questions available" + suffix + " — add some first");
        }

        RoomGame game = roomGameRepository.findByRoomId(roomId).orElse(null);
        if (game == null) {
            game = new RoomGame();
            game.setRoom(room);
        }

        LocalDateTime now = LocalDateTime.now();
        game.setCurrentQuestionIndex(0);
        game.setStartedAt(now);
        game.setQuestionStartedAt(now);
        game.setDeadlineAt(now.plusMinutes(ROUND_DURATION_MINUTES));
        game = roomGameRepository.save(game);

        roomGameQuestionRepository.deleteAllByRoomGameId(game.getId());
        roomGameQuestionRepository.flush();

        for (int i = 0; i < selected.size(); i++) {
            RoomGameQuestion rgq = new RoomGameQuestion();
            rgq.setRoomGame(game);
            rgq.setQuestion(selected.get(i));
            rgq.setPosition(i);
            roomGameQuestionRepository.save(rgq);
        }

        markUsed(room, selected.get(0));

        room.setStatus("IN_PROGRESS");
        roomRepository.save(room);

        GameQuestionResponse resp = toResponse(room, game, selected.get(0), 0, selected.size());
        publisher.publishStatus(room.getId(), "IN_PROGRESS");
        publisher.publishQuestion(room.getId(), resp);
        return resp;
    }

    /**
     * Adds more questions to an already-running game. Used when the per-question
     * timer runs out at the last picked question but the round timer hasn't expired:
     * we keep the round going by drawing fresh non-repeated questions.
     *
     * Returns the number of questions actually appended (0 if the pool is empty
     * even after the used-history wipe).
     */
    @Transactional
    public int appendQuestions(Long roomId, int count) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return 0;
        RoomGame game = roomGameRepository.findByRoomId(roomId).orElse(null);
        if (game == null) return 0;

        int existingCount = roomGameQuestionRepository
                .findAllByRoomGameIdOrderByPositionAsc(game.getId()).size();

        List<Question> selected = pickQuestions(room, count);
        if (selected.isEmpty()) return 0;

        for (int i = 0; i < selected.size(); i++) {
            RoomGameQuestion rgq = new RoomGameQuestion();
            rgq.setRoomGame(game);
            rgq.setQuestion(selected.get(i));
            rgq.setPosition(existingCount + i);
            roomGameQuestionRepository.save(rgq);
        }
        return selected.size();
    }

    public GameQuestionResponse getCurrentQuestion(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        RoomGame game = roomGameRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Game not started"));

        RoomGameQuestion rgq = roomGameQuestionRepository
                .findByRoomGameIdAndPosition(game.getId(), game.getCurrentQuestionIndex())
                .orElseThrow(() -> new RuntimeException("Current question not found"));

        int total = roomGameQuestionRepository.findAllByRoomGameIdOrderByPositionAsc(game.getId()).size();
        return toResponse(room, game, rgq.getQuestion(), game.getCurrentQuestionIndex(), total);
    }

    /**
     * Picks up to `count` questions for the room, optionally filtered by difficulty,
     * excluding questions already used in previous games of this room. If the
     * filtered pool is exhausted, the used-history is wiped and the pool is
     * recomputed from scratch.
     */
    private List<Question> pickQuestions(Room room, int count) {
        List<Question> pool = (room.getDifficulty() != null && !room.getDifficulty().isBlank())
                ? questionRepository.findByDifficultyIgnoreCase(room.getDifficulty())
                : questionRepository.findAll();
        if (pool.isEmpty()) return pool;

        Set<Long> used = usedQuestionsByRoom.getOrDefault(room.getId(), Set.of());
        if (!used.isEmpty()) {
            List<Question> filtered = pool.stream().filter(q -> !used.contains(q.getId())).toList();
            if (filtered.isEmpty()) {
                // pool exhausted — wipe history so the next pick can reuse from scratch
                usedQuestionsByRoom.remove(room.getId());
            } else {
                pool = filtered;
            }
        }

        List<Question> mutable = new ArrayList<>(pool);
        Collections.shuffle(mutable);
        if (count > mutable.size()) count = mutable.size();
        return new ArrayList<>(mutable.subList(0, count));
    }

    /**
     * Records a question as "shown" in this room. Idempotent — adding twice is a no-op.
     */
    public void markUsed(Room room, Question q) {
        usedQuestionsByRoom
                .computeIfAbsent(room.getId(), k -> ConcurrentHashMap.newKeySet())
                .add(q.getId());
    }

    private Player currentPlayer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Not authenticated");
        }
        return playerRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }

    private GameQuestionResponse toResponse(Room room, RoomGame game, Question q, int index, int total) {
        return new GameQuestionResponse(
                room.getId(),
                q.getId(),
                q.getPrompt(),
                q.getDifficulty(),
                index,
                total,
                game.getQuestionStartedAt(),
                game.getDeadlineAt()
        );
    }
}
