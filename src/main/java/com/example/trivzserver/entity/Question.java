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
    private String difficulty;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    private List<QuestionAnswer> acceptedAnswers = new ArrayList<>();

    public Question() {
    }

    public Question(Long id, String prompt, String difficulty, List<QuestionAnswer> acceptedAnswers) {
        this.id = id;
        this.prompt = prompt;
        this.difficulty = difficulty;
        this.acceptedAnswers = acceptedAnswers;
    }

    public Long getId() { return id; }
    public String getPrompt() { return prompt; }
    public String getDifficulty() { return difficulty; }
    public List<QuestionAnswer> getAcceptedAnswers() { return acceptedAnswers; }

    public void setId(Long id) { this.id = id; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setAcceptedAnswers(List<QuestionAnswer> acceptedAnswers) { this.acceptedAnswers = acceptedAnswers; }
}
