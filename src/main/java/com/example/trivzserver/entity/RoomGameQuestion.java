package com.example.trivzserver.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "room_game_questions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_game_id", "position"}))
public class RoomGameQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "room_game_id", nullable = false)
    private RoomGame roomGame;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private Integer position;

    public RoomGameQuestion() {}

    public Long getId() { return id; }
    public RoomGame getRoomGame() { return roomGame; }
    public Question getQuestion() { return question; }
    public Integer getPosition() { return position; }

    public void setId(Long id) { this.id = id; }
    public void setRoomGame(RoomGame roomGame) { this.roomGame = roomGame; }
    public void setQuestion(Question question) { this.question = question; }
    public void setPosition(Integer position) { this.position = position; }
}