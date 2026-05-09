package com.example.trivzserver.service;

import com.example.trivzserver.dto.RoomMemberResponse;
import com.example.trivzserver.entity.Player;
import com.example.trivzserver.entity.Room;
import com.example.trivzserver.entity.RoomMember;
import com.example.trivzserver.repository.PlayerRepository;
import com.example.trivzserver.repository.RoomMemberRepository;
import com.example.trivzserver.repository.RoomRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomMemberService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final GameEventPublisher publisher;

    public RoomMemberService(RoomRepository roomRepository,
                             PlayerRepository playerRepository,
                             RoomMemberRepository roomMemberRepository,
                             GameEventPublisher publisher) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.publisher = publisher;
    }

    public List<RoomMember> listMembers(Long roomId) {
        roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));
        return roomMemberRepository.findByRoomId(roomId);
    }

    public RoomMember join(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        String s = room.getStatus();
        if (!"LOBBY".equalsIgnoreCase(s) && !"IN_PROGRESS".equalsIgnoreCase(s)) {
            throw new RuntimeException("Cannot join — this room has finished");
        }

        Player player = getCurrentPlayer();

        return roomMemberRepository.findByRoomIdAndPlayerId(roomId, player.getId())
                .orElseGet(() -> {
                    long currentCount = roomMemberRepository.countByRoomId(roomId);
                    if (room.getMaxPlayers() != null && currentCount >= room.getMaxPlayers()) {
                        throw new RuntimeException("Room is full");
                    }

                    RoomMember member = new RoomMember();
                    member.setRoom(room);
                    member.setPlayer(player);
                    RoomMember saved = roomMemberRepository.save(member);
                    publisher.publishMember(roomId, "JOIN", toResponse(saved));
                    return saved;
                });
    }

    public void leave(Long roomId) {
        Player player = getCurrentPlayer();

        RoomMember member = roomMemberRepository.findByRoomIdAndPlayerId(roomId, player.getId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        RoomMemberResponse snapshot = toResponse(member);
        roomMemberRepository.delete(member);
        publisher.publishMember(roomId, "LEAVE", snapshot);
    }

    private RoomMemberResponse toResponse(RoomMember member) {
        return new RoomMemberResponse(
                member.getId(),
                member.getRoom().getId(),
                member.getPlayer().getId(),
                member.getPlayer().getUsername(),
                member.getJoinedAt()
        );
    }

    private Player getCurrentPlayer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Not authenticated");
        }

        String username = auth.getName();
        return playerRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }
}