package com.example.trivzserver.dto;

import java.time.LocalDateTime;

public class RoomResponse {
    private Long id;
    private String code;
    private String name;
    private Integer maxPlayers;
    private String status;
    private LocalDateTime createdAt;

    public RoomResponse() {
    }

    public RoomResponse(Long id, String code, String name, Integer maxPlayers, String status, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}