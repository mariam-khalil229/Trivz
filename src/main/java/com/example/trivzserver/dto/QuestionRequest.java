package com.example.trivzserver.dto;

import java.util.List;

public class QuestionRequest {
    private String prompt;
    private String difficulty;
    private List<String> acceptedAnswers;

    public QuestionRequest() {
    }

    public String getPrompt() { return prompt; }
    public String getDifficulty() { return difficulty; }
    public List<String> getAcceptedAnswers() { return acceptedAnswers; }

    public void setPrompt(String prompt) { this.prompt = prompt; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setAcceptedAnswers(List<String> acceptedAnswers) { this.acceptedAnswers = acceptedAnswers; }
}
