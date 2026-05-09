package com.example.trivzserver.service;

import com.example.trivzserver.dto.LeaderboardEntry;
import com.example.trivzserver.dto.ScoreResponse;
import com.example.trivzserver.dto.SubmitAnswerRequest;
import com.example.trivzserver.entity.Player;
import com.example.trivzserver.entity.Question;
import com.example.trivzserver.entity.Room;
import com.example.trivzserver.entity.RoomGame;
import com.example.trivzserver.entity.RoomGameQuestion;
import com.example.trivzserver.entity.Score;
import com.example.trivzserver.repository.PlayerRepository;
import com.example.trivzserver.repository.RoomGameQuestionRepository;
import com.example.trivzserver.repository.RoomGameRepository;
import com.example.trivzserver.repository.RoomRepository;
import com.example.trivzserver.repository.ScoreRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScoreService {

    public static final long WIN_THRESHOLD = 100L;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final ScoreRepository scoreRepository;
    private final AnswerService answerService;
    private final RoomGameRepository roomGameRepository;
    private final RoomGameQuestionRepository roomGameQuestionRepository;
    private final GameEventPublisher publisher;

    public ScoreService(RoomRepository roomRepository,
                        PlayerRepository playerRepository,
                        ScoreRepository scoreRepository,
                        AnswerService answerService,
                        RoomGameRepository roomGameRepository,
                        RoomGameQuestionRepository roomGameQuestionRepository,
                        GameEventPublisher publisher) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.scoreRepository = scoreRepository;
        this.answerService = answerService;
        this.roomGameRepository = roomGameRepository;
        this.roomGameQuestionRepository = roomGameQuestionRepository;
        this.publisher = publisher;
    }

    public ScoreResponse submit(Long roomId, SubmitAnswerRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!"IN_PROGRESS".equalsIgnoreCase(room.getStatus())) {
            throw new RuntimeException("Game is not in progress");
        }

        Player player = getCurrentPlayer();

        RoomGame game = roomGameRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Game not started"));

        if (game.getRevealUntil() != null && LocalDateTime.now().isBefore(game.getRevealUntil())) {
            throw new RuntimeException("Answer reveal in progress — wait for the next question");
        }

        RoomGameQuestion rgq = roomGameQuestionRepository
                .findByRoomGameIdAndPosition(game.getId(), game.getCurrentQuestionIndex())
                .orElseThrow(() -> new RuntimeException("Current question not found"));

        Question currentQuestion = rgq.getQuestion();

        if (request.getQuestionId() == null) {
            throw new RuntimeException("questionId is required");
        }
        if (!request.getQuestionId().equals(currentQuestion.getId())) {
            throw new RuntimeException("Submitted question does not match current question");
        }

        // Each round wipes scores in GameService.startGame, so a simple dedupe
        // by (room, player, question) is enough — there are no historical rows
        // from previous rounds left to collide with.
        boolean alreadySubmitted = scoreRepository.existsByRoomIdAndPlayerIdAndQuestionId(
                roomId, player.getId(), currentQuestion.getId());
        if (alreadySubmitted) {
            throw new RuntimeException("Answer already submitted for this question");
        }

        boolean correct = answerService.isCorrectAnswer(currentQuestion, request.getAnswerText());

        int timeTaken = request.getTimeTakenSeconds() != null ? request.getTimeTakenSeconds() : 0;
        int timeLimit = GameService.QUESTION_TIME_LIMIT_SECONDS;

        int points = (correct && timeTaken <= timeLimit) ? 10 : 0;

        Score score = new Score();
        score.setRoom(room);
        score.setPlayer(player);
        score.setQuestion(currentQuestion);
        score.setCorrect(correct);
        score.setPoints(points);

        Score saved = scoreRepository.save(score);

        ScoreResponse resp = new ScoreResponse(
                saved.getId(),
                saved.getPlayer().getId(),
                saved.getPlayer().getUsername(),
                saved.getRoom().getId(),
                saved.getQuestion().getId(),
                saved.getCorrect(),
                saved.getPoints(),
                saved.getAnsweredAt()
        );

        publisher.publishScore(roomId, resp);
        List<LeaderboardEntry> board = scoreRepository.getLeaderboard(roomId);
        publisher.publishLeaderboard(roomId, board);

        if (!board.isEmpty()
                && board.get(0).getTotalPoints() != null
                && board.get(0).getTotalPoints() >= WIN_THRESHOLD) {
            // Re-fetch the latest status so concurrent submits don't both transition
            // IN_PROGRESS → FINISHED and double-publish the status event.
            Room latest = roomRepository.findById(roomId).orElse(room);
            if ("IN_PROGRESS".equalsIgnoreCase(latest.getStatus())) {
                latest.setStatus("FINISHED");
                roomRepository.save(latest);
                publisher.publishStatus(roomId, "FINISHED");
            }
        }

        return resp;
    }

    public List<LeaderboardEntry> leaderboard(Long roomId) {
        return scoreRepository.getLeaderboard(roomId);
    }

    private Player getCurrentPlayer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Not authenticated");
        }

        String username = auth.getName();
        return playerRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }
}
