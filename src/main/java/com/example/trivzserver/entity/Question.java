package com.example.trivzserver.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String prompt;

    @Column
    private String category;

    @Column
    private String difficulty;

    @Column
    private Integer timeLimitSeconds = 20;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionAnswer> acceptedAnswers = new ArrayList<>();

    public Question() {
    }

    public Question(Long id, String prompt, String category, String difficulty, Integer timeLimitSeconds, List<QuestionAnswer> acceptedAnswers) {
        this.id = id;
        this.prompt = prompt;
        this.category = category;
        this.difficulty = difficulty;
        this.timeLimitSeconds = timeLimitSeconds;
        this.acceptedAnswers = acceptedAnswers;
    }

    public Long getId() {
        return id;
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

    public List<QuestionAnswer> getAcceptedAnswers() {
        return acceptedAnswers;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setAcceptedAnswers(List<QuestionAnswer> acceptedAnswers) {
        this.acceptedAnswers = acceptedAnswers;
    }
}