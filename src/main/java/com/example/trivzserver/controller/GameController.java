package com.example.trivzserver.controller;

import com.example.trivzserver.dto.GameQuestionResponse;
import com.example.trivzserver.dto.StartGameRequest;
import com.example.trivzserver.service.GameService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{roomId}/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/start")
    public GameQuestionResponse start(@PathVariable Long roomId, @RequestBody(required = false) StartGameRequest request) {
        Integer count = (request == null) ? null : request.getQuestionCount();
        return gameService.startGame(roomId, count);
    }

    @GetMapping("/current")
    public GameQuestionResponse current(@PathVariable Long roomId) {
        return gameService.getCurrentQuestion(roomId);
    }
}
