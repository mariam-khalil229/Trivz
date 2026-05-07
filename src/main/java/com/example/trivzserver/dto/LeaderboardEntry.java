package com.example.trivzserver.dto;

public class LeaderboardEntry {
    private Long playerId;
    private String username;
    private Long totalPoints; // <- must be Long for SUM

    public LeaderboardEntry() {
    }

    public LeaderboardEntry(Long playerId, String username, Long totalPoints) {
        this.playerId = playerId;
        this.username = username;
        this.totalPoints = totalPoints;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public Long getTotalPoints() {
        return totalPoints;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setTotalPoints(Long totalPoints) {
        this.totalPoints = totalPoints;
    }
}