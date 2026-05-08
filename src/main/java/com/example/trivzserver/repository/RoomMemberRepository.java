package com.example.trivzserver.repository;

import com.example.trivzserver.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByRoomId(Long roomId);
    Optional<RoomMember> findByRoomIdAndPlayerId(Long roomId, Long playerId);

    long countByRoomId(Long roomId);
}