package com.example.trivzserver.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scores")
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false)
    private Integer points = 0;

    @Column(nullable = false)
    private Boolean correct = false;

    @Column
    private LocalDateTime answeredAt = LocalDateTime.now();

    public Score() {
    }

    public Score(Long id, Player player, Room room, Question question, Integer points, Boolean correct, LocalDateTime answeredAt) {
        this.id = id;
        this.player = player;
        this.room = room;
        this.question = question;
        this.points = points;
        this.correct = correct;
        this.answeredAt = answeredAt;
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public Room getRoom() {
        return room;
    }

    public Question getQuestion() {
        return question;
    }

    public Integer getPoints() {
        return points;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
}