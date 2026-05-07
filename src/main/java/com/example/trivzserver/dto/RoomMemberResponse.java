package com.example.trivzserver.dto;

import java.time.LocalDateTime;

public class RoomMemberResponse {
    private Long id;
    private Long roomId;
    private Long playerId;
    private String playerUsername;
    private LocalDateTime joinedAt;

    public RoomMemberResponse() {
    }

    public RoomMemberResponse(Long id, Long roomId, Long playerId, String playerUsername, LocalDateTime joinedAt) {
        this.id = id;
        this.roomId = roomId;
        this.playerId = playerId;
        this.playerUsername = playerUsername;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public void setPlayerUsername(String playerUsername) {
        this.playerUsername = playerUsername;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}