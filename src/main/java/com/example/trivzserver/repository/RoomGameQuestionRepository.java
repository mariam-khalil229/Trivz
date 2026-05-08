package com.example.trivzserver.repository;

import com.example.trivzserver.entity.RoomGameQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomGameQuestionRepository extends JpaRepository<RoomGameQuestion, Long> {
    List<RoomGameQuestion> findAllByRoomGameIdOrderByPositionAsc(Long roomGameId);
    Optional<RoomGameQuestion> findByRoomGameIdAndPosition(Long roomGameId, Integer position);
    void deleteAllByRoomGameId(Long roomGameId);
}