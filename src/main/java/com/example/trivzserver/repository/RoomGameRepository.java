package com.example.trivzserver.repository;

import com.example.trivzserver.entity.RoomGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomGameRepository extends JpaRepository<RoomGame, Long> {
    Optional<RoomGame> findByRoomId(Long roomId);
}