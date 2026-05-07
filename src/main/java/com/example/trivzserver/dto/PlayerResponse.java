package com.example.trivzserver.dto;

import com.example.trivzserver.entity.Player;

import java.time.LocalDateTime;

public class PlayerResponse {
    private Long id;
    private String username;
    private String displayName;
    private LocalDateTime createdAt;

    public PlayerResponse() {}

    public static PlayerResponse fromEntity(Player player) {
        PlayerResponse res = new PlayerResponse();
        res.setId(player.getId());
        res.setUsername(player.getUsername());
        res.setDisplayName(player.getDisplayName());
        res.setCreatedAt(player.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}