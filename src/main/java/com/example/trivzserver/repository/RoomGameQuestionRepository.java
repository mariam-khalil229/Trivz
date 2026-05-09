package com.example.trivzserver.repository;

import com.example.trivzserver.entity.RoomGameQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomGameQuestionRepository extends JpaRepository<RoomGameQuestion, Long> {
    List<RoomGameQuestion> findAllByRoomGameIdOrderByPositionAsc(Long roomGameId);
    Optional<RoomGameQuestion> findByRoomGameIdAndPosition(Long roomGameId, Integer position);

    @Modifying
    @Query("delete from RoomGameQuestion r where r.roomGame.id = :id")
    void deleteAllByRoomGameId(@Param("id") Long id);

    @Modifying
    @Query("delete from RoomGameQuestion r where r.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);

    /**
     * True iff this question appears in any room whose status is currently IN_PROGRESS.
     */
    @Query("select count(r) from RoomGameQuestion r " +
           "where r.question.id = :questionId and r.roomGame.room.status = :status")
    long countByQuestionAndRoomStatus(@Param("questionId") Long questionId, @Param("status") String status);
}
