package com.example.trivzserver.service;

import com.example.trivzserver.dto.GameQuestionResponse;
import com.example.trivzserver.dto.LeaderboardEntry;
import com.example.trivzserver.dto.RoomMemberResponse;
import com.example.trivzserver.dto.ScoreResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GameEventPublisher {

    private final SimpMessagingTemplate template;

    public GameEventPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    @Async
    public void publishQuestion(Long roomId, GameQuestionResponse q) {
        template.convertAndSend("/topic/room/" + roomId + "/question", q);
    }

    @Async
    public void publishScore(Long roomId, ScoreResponse score) {
        template.convertAndSend("/topic/room/" + roomId + "/score", score);
    }

    @Async
    public void publishLeaderboard(Long roomId, List<LeaderboardEntry> board) {
        template.convertAndSend("/topic/room/" + roomId + "/leaderboard", board);
    }

    @Async
    public void publishStatus(Long roomId, String status) {
        Object payload = Map.of("status", status);
        template.convertAndSend("/topic/room/" + roomId + "/status", payload);
    }

    @Async
    public void publishMember(Long roomId, String event, RoomMemberResponse member) {
        Object payload = Map.of("event", event, "member", member);
        template.convertAndSend("/topic/room/" + roomId + "/members", payload);
    }

    @Async
    public void publishTick(Long roomId, int remainingSeconds, int questionIndex) {
        Object payload = Map.of("remaining", remainingSeconds, "index", questionIndex);
        template.convertAndSend("/topic/room/" + roomId + "/tick", payload);
    }

    @Async
    public void publishReveal(Long roomId, Long questionId, String answer, int seconds) {
        Object payload = Map.of(
                "questionId", questionId,
                "answer", answer,
                "seconds", seconds);
        template.convertAndSend("/topic/room/" + roomId + "/reveal", payload);
    }
}
