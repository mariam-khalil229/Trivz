package com.example.trivzserver.controller;

import com.example.trivzserver.dto.LeaderboardEntry;
import com.example.trivzserver.dto.ScoreResponse;
import com.example.trivzserver.dto.SubmitAnswerRequest;
import com.example.trivzserver.service.ScoreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/scores")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @PostMapping("/submit")
    public ScoreResponse submit(@PathVariable Long roomId, @RequestBody SubmitAnswerRequest request) {
        return scoreService.submit(roomId, request);
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard(@PathVariable Long roomId) {
        return scoreService.leaderboard(roomId);
    }
}