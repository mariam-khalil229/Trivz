package com.example.trivzserver.service;

import com.example.trivzserver.dto.GameQuestionResponse;
import com.example.trivzserver.entity.Question;
import com.example.trivzserver.entity.QuestionAnswer;
import com.example.trivzserver.entity.Room;
import com.example.trivzserver.entity.RoomGame;
import com.example.trivzserver.entity.RoomGameQuestion;
import com.example.trivzserver.repository.RoomGameQuestionRepository;
import com.example.trivzserver.repository.RoomGameRepository;
import com.example.trivzserver.repository.RoomMemberRepository;
import com.example.trivzserver.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class GameTimerService {

    public static final int REVEAL_SECONDS = 5;

    private final RoomRepository roomRepository;
    private final RoomGameRepository roomGameRepository;
    private final RoomGameQuestionRepository roomGameQuestionRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ScoreService scoreService;
    private final GameService gameService;
    private final GameEventPublisher publisher;

    private final ReentrantLock tickLock = new ReentrantLock();

    public GameTimerService(RoomRepository roomRepository,
                            RoomGameRepository roomGameRepository,
                            RoomGameQuestionRepository roomGameQuestionRepository,
                            RoomMemberRepository roomMemberRepository,
                            ScoreService scoreService,
                            GameService gameService,
                            GameEventPublisher publisher) {
        this.roomRepository = roomRepository;
        this.roomGameRepository = roomGameRepository;
        this.roomGameQuestionRepository = roomGameQuestionRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.scoreService = scoreService;
        this.gameService = gameService;
        this.publisher = publisher;
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void tick() {
        if (!tickLock.tryLock()) {
            return;
        }
        try {
            List<Room> active = roomRepository.findAll().stream()
                    .filter(r -> "IN_PROGRESS".equalsIgnoreCase(r.getStatus()))
                    .toList();

            for (Room room : active) {
                processRoom(room);
            }
        } finally {
            tickLock.unlock();
        }
    }

    private void processRoom(Room room) {
        RoomGame game = roomGameRepository.findByRoomId(room.getId()).orElse(null);
        if (game == null || game.getQuestionStartedAt() == null) return;

        LocalDateTime now = LocalDateTime.now();

        // Empty room — every player has left. End the round.
        if (roomMemberRepository.countByRoomId(room.getId()) == 0) {
            endGame(room);
            return;
        }

        // Round deadline (3-min) — counts down except during reveal (deadline gets pushed by 5s)
        if (game.getDeadlineAt() != null && now.isAfter(game.getDeadlineAt())) {
            endGame(room);
            return;
        }

        // Reveal phase: hold until revealUntil, then advance
        if (game.getRevealUntil() != null) {
            if (now.isAfter(game.getRevealUntil())) {
                game.setRevealUntil(null);
                roomGameRepository.save(game);

                List<RoomGameQuestion> ordered =
                        roomGameQuestionRepository.findAllByRoomGameIdOrderByPositionAsc(game.getId());
                advance(room, game, ordered, game.getCurrentQuestionIndex());
            }
            return;
        }

        List<RoomGameQuestion> ordered =
                roomGameQuestionRepository.findAllByRoomGameIdOrderByPositionAsc(game.getId());
        if (ordered.isEmpty()) return;

        int idx = game.getCurrentQuestionIndex();
        if (idx >= ordered.size()) return;

        Question current = ordered.get(idx).getQuestion();
        int limit = GameService.QUESTION_TIME_LIMIT_SECONDS;

        long elapsed = Duration.between(game.getQuestionStartedAt(), now).getSeconds();
        int remaining = (int) Math.max(0, limit - elapsed);

        publisher.publishTick(room.getId(), remaining, idx);

        if (remaining <= 0) {
            enterReveal(room, game, current);
        }
    }

    private void enterReveal(Room room, RoomGame game, Question current) {
        LocalDateTime now = LocalDateTime.now();
        game.setRevealUntil(now.plusSeconds(REVEAL_SECONDS));

        // Pause the round clock by pushing the deadline forward
        if (game.getDeadlineAt() != null) {
            game.setDeadlineAt(game.getDeadlineAt().plusSeconds(REVEAL_SECONDS));
        }

        roomGameRepository.save(game);

        String answer = current.getAcceptedAnswers().isEmpty()
                ? "(no answer recorded)"
                : current.getAcceptedAnswers().get(0).getAnswerText();

        publisher.publishReveal(room.getId(), current.getId(), answer, REVEAL_SECONDS);
    }

    private void advance(Room room, RoomGame game, List<RoomGameQuestion> ordered, int idx) {
        int next = idx + 1;

        // Out of pre-picked questions — try to extend the game so the round timer
        // (or the 100-pt threshold, or the room going empty) is what ends the round,
        // not us simply running out of pre-shuffled questions.
        if (next >= ordered.size()) {
            int appended = gameService.appendQuestions(room.getId(), 5);
            if (appended == 0) {
                endGame(room);
                return;
            }
            ordered = roomGameQuestionRepository.findAllByRoomGameIdOrderByPositionAsc(game.getId());
        }

        game.setCurrentQuestionIndex(next);
        game.setQuestionStartedAt(LocalDateTime.now());
        roomGameRepository.save(game);

        Question q = ordered.get(next).getQuestion();
        gameService.markUsed(room, q);

        GameQuestionResponse resp = new GameQuestionResponse(
                room.getId(),
                q.getId(),
                q.getPrompt(),
                q.getDifficulty(),
                next,
                ordered.size(),
                game.getQuestionStartedAt(),
                game.getDeadlineAt()
        );
        publisher.publishQuestion(room.getId(), resp);
    }

    private void endGame(Room room) {
        room.setStatus("FINISHED");
        roomRepository.save(room);
        publisher.publishStatus(room.getId(), "FINISHED");
        publisher.publishLeaderboard(room.getId(), scoreService.leaderboard(room.getId()));
    }
}
