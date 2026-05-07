package com.example.trivzserver.dto;

public class RoomRequest {
    private String name;
    private Integer maxPlayers;

    public RoomRequest() {
    }

    public RoomRequest(String name, Integer maxPlayers) {
        this.name = name;
        this.maxPlayers = maxPlayers;
    }

    public String getName() {
        return name;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}