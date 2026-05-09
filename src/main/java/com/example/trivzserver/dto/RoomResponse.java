package com.example.trivzserver.dto;

import java.time.LocalDateTime;

public class RoomResponse {
    private Long id;
    private String code;
    private String name;
    private Integer maxPlayers;
    private String status;
    private Long hostPlayerId;
    private String hostUsername;
    private Long memberCount;
    private String difficulty;
    private LocalDateTime createdAt;

    public RoomResponse() {
    }

    public RoomResponse(Long id, String code, String name, Integer maxPlayers, String status,
                        Long hostPlayerId, String hostUsername, Long memberCount,
                        String difficulty, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.status = status;
        this.hostPlayerId = hostPlayerId;
        this.hostUsername = hostUsername;
        this.memberCount = memberCount;
        this.difficulty = difficulty;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getMaxPlayers() { return maxPlayers; }
    public String getStatus() { return status; }
    public Long getHostPlayerId() { return hostPlayerId; }
    public String getHostUsername() { return hostUsername; }
    public Long getMemberCount() { return memberCount; }
    public String getDifficulty() { return difficulty; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    public void setStatus(String status) { this.status = status; }
    public void setHostPlayerId(Long hostPlayerId) { this.hostPlayerId = hostPlayerId; }
    public void setHostUsername(String hostUsername) { this.hostUsername = hostUsername; }
    public void setMemberCount(Long memberCount) { this.memberCount = memberCount; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
