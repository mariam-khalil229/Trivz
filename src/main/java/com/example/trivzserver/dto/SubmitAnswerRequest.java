package com.example.trivzserver.dto;

public class SubmitAnswerRequest {
    private Long playerId;
    private Long questionId;
    private String answerText;
    private Integer timeTakenSeconds;

    public SubmitAnswerRequest() {
    }

    public SubmitAnswerRequest(Long playerId, Long questionId, String answerText, Integer timeTakenSeconds) {
        this.playerId = playerId;
        this.questionId = questionId;
        this.answerText = answerText;
        this.timeTakenSeconds = timeTakenSeconds;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getAnswerText() {
        return answerText;
    }

    public Integer getTimeTakenSeconds() {
        return timeTakenSeconds;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public void setTimeTakenSeconds(Integer timeTakenSeconds) {
        this.timeTakenSeconds = timeTakenSeconds;
    }
}