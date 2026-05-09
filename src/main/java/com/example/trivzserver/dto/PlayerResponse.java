package com.example.trivzserver.dto;

import com.example.trivzserver.entity.Player;

import java.time.LocalDateTime;

public class PlayerResponse {
    private Long id;
    private String username;
    private String role;
    private LocalDateTime createdAt;

    public PlayerResponse() {}

    public static PlayerResponse fromEntity(Player player) {
        PlayerResponse res = new PlayerResponse();
        res.setId(player.getId());
        res.setUsername(player.getUsername());
        res.setRole(player.getRole());
        res.setCreatedAt(player.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
