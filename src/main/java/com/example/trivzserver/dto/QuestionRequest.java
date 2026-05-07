package com.example.trivzserver.dto;

import java.util.List;

public class QuestionRequest {
    private String prompt;
    private String category;
    private String difficulty;
    private Integer timeLimitSeconds;
    private List<String> acceptedAnswers;

    public QuestionRequest() {
    }

    public QuestionRequest(String prompt, String category, String difficulty, Integer timeLimitSeconds, List<String> acceptedAnswers) {
        this.prompt = prompt;
        this.category = category;
        this.difficulty = difficulty;
        this.timeLimitSeconds = timeLimitSeconds;
        this.acceptedAnswers = acceptedAnswers;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getCategory() {
        return category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public List<String> getAcceptedAnswers() {
        return acceptedAnswers;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public void setAcceptedAnswers(List<String> acceptedAnswers) {
        this.acceptedAnswers = acceptedAnswers;
    }
}