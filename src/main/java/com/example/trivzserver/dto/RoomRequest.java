package com.example.trivzserver.dto;

public class RoomRequest {
    private String name;
    private Integer maxPlayers;
    private String difficulty;

    public RoomRequest() {
    }

    public String getName() { return name; }
    public Integer getMaxPlayers() { return maxPlayers; }
    public String getDifficulty() { return difficulty; }

    public void setName(String name) { this.name = name; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}
