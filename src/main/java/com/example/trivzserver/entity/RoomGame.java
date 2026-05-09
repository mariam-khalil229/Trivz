package com.example.trivzserver.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_games")
public class RoomGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    @Column(nullable = false)
    private Integer currentQuestionIndex = 0;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime questionStartedAt;

    @Column
    private LocalDateTime deadlineAt;

    @Column
    private LocalDateTime revealUntil;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RoomGame() {}

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public Integer getCurrentQuestionIndex() { return currentQuestionIndex; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getQuestionStartedAt() { return questionStartedAt; }
    public LocalDateTime getDeadlineAt() { return deadlineAt; }
    public LocalDateTime getRevealUntil() { return revealUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setRoom(Room room) { this.room = room; }
    public void setCurrentQuestionIndex(Integer currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public void setQuestionStartedAt(LocalDateTime questionStartedAt) { this.questionStartedAt = questionStartedAt; }
    public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
    public void setRevealUntil(LocalDateTime revealUntil) { this.revealUntil = revealUntil; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
