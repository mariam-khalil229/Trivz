package com.example.trivzserver.controller;

import com.example.trivzserver.dto.ScoreResponse;
import com.example.trivzserver.dto.SubmitAnswerRequest;
import com.example.trivzserver.entity.Player;
import com.example.trivzserver.entity.Question;
import com.example.trivzserver.entity.Room;
import com.example.trivzserver.entity.Score;
import com.example.trivzserver.repository.PlayerRepository;
import com.example.trivzserver.repository.QuestionRepository;
import com.example.trivzserver.repository.RoomRepository;
import com.example.trivzserver.repository.ScoreRepository;
import com.example.trivzserver.service.AnswerService;
import org.springframework.web.bind.annotation.*;
import com.example.trivzserver.dto.LeaderboardEntry;

@RestController
@RequestMapping("/api/rooms/{roomId}/scores")
public class ScoreController {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final ScoreRepository scoreRepository;
    private final AnswerService answerService;

    public ScoreController(RoomRepository roomRepository,
                           PlayerRepository playerRepository,
                           QuestionRepository questionRepository,
                           ScoreRepository scoreRepository,
                           AnswerService answerService) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.questionRepository = questionRepository;
        this.scoreRepository = scoreRepository;
        this.answerService = answerService;
    }

    @PostMapping("/submit")
    public ScoreResponse submit(@PathVariable Long roomId, @RequestBody SubmitAnswerRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        boolean correct = answerService.isCorrectAnswer(question, request.getAnswerText());

        int timeTaken = request.getTimeTakenSeconds() != null ? request.getTimeTakenSeconds() : 0;
        int timeLimit = question.getTimeLimitSeconds() != null ? question.getTimeLimitSeconds() : 20;

        int points = (correct && timeTaken <= timeLimit) ? 10 : 0;

        Score score = new Score();
        score.setRoom(room);
        score.setPlayer(player);
        score.setQuestion(question);
        score.setCorrect(correct);
        score.setPoints(points);

        Score saved = scoreRepository.save(score);

        return new ScoreResponse(
                saved.getId(),
                saved.getPlayer().getId(),
                saved.getRoom().getId(),
                saved.getQuestion().getId(),
                saved.getCorrect(),
                saved.getPoints(),
                saved.getAnsweredAt()
        );
    }

    @GetMapping("/leaderboard")
    public java.util.List<LeaderboardEntry> leaderboard(@PathVariable Long roomId) {
        return scoreRepository.getLeaderboard(roomId);
    }
}