package com.example.trivzserver.repository;

import com.example.trivzserver.dto.LeaderboardEntry;
import com.example.trivzserver.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {

    @Query("""
        select new com.example.trivzserver.dto.LeaderboardEntry(
            s.player.id,
            s.player.username,
            sum(s.points)
        )
        from Score s
        where s.room.id = :roomId
        group by s.player.id, s.player.username
        order by sum(s.points) desc
    """)
    List<LeaderboardEntry> getLeaderboard(@Param("roomId") Long roomId);

    boolean existsByRoomIdAndPlayerIdAndQuestionId(Long roomId, Long playerId, Long questionId);

    @Modifying
    @Query("delete from Score s where s.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);

    @Modifying
    @Query("delete from Score s where s.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}
