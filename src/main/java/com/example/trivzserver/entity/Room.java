package com.example.trivzserver.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column
    private Integer maxPlayers = 8;

    @Column
    private String status;

    @Column
    private Long hostPlayerId;

    @Column
    private String difficulty;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();

    public Room() {
    }

    public Room(Long id, String code, String name, Integer maxPlayers, String status, Long hostPlayerId, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.status = status;
        this.hostPlayerId = hostPlayerId;
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

    public Long getHostPlayerId() {
        return hostPlayerId;
    }

    public String getDifficulty() {
        return difficulty;
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

    public void setHostPlayerId(Long hostPlayerId) {
        this.hostPlayerId = hostPlayerId;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
