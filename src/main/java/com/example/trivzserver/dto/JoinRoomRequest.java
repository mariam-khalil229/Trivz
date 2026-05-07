package com.example.trivzserver.dto;

public class JoinRoomRequest {
    private Long playerId;

    public JoinRoomRequest() {
    }

    public JoinRoomRequest(Long playerId) {
        this.playerId = playerId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }
}