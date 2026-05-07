package com.example.trivzserver.dto;

import java.time.LocalDateTime;

public class ScoreResponse {
    private Long id;
    private Long playerId;
    private Long roomId;
    private Long questionId;
    private Boolean correct;
    private Integer points;
    private LocalDateTime answeredAt;

    public ScoreResponse() {
    }

    public ScoreResponse(Long id, Long playerId, Long roomId, Long questionId, Boolean correct, Integer points, LocalDateTime answeredAt) {
        this.id = id;
        this.playerId = playerId;
        this.roomId = roomId;
        this.questionId = questionId;
        this.correct = correct;
        this.points = points;
        this.answeredAt = answeredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public Integer getPoints() {
        return points;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
}