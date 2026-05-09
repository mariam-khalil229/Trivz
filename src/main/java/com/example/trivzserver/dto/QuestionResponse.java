package com.example.trivzserver.dto;

import java.util.List;

public class QuestionResponse {
    private Long id;
    private String prompt;
    private String difficulty;
    private List<String> acceptedAnswers;

    public QuestionResponse() {
    }

    public QuestionResponse(Long id, String prompt, String difficulty, List<String> acceptedAnswers) {
        this.id = id;
        this.prompt = prompt;
        this.difficulty = difficulty;
        this.acceptedAnswers = acceptedAnswers;
    }

    public Long getId() { return id; }
    public String getPrompt() { return prompt; }
    public String getDifficulty() { return difficulty; }
    public List<String> getAcceptedAnswers() { return acceptedAnswers; }

    public void setId(Long id) { this.id = id; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setAcceptedAnswers(List<String> acceptedAnswers) { this.acceptedAnswers = acceptedAnswers; }
}
