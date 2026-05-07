package com.example.trivzserver.controller;

import com.example.trivzserver.dto.JoinRoomRequest;
import com.example.trivzserver.dto.RoomMemberResponse;
import com.example.trivzserver.entity.Player;
import com.example.trivzserver.entity.Room;
import com.example.trivzserver.entity.RoomMember;
import com.example.trivzserver.repository.PlayerRepository;
import com.example.trivzserver.repository.RoomMemberRepository;
import com.example.trivzserver.repository.RoomRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/members")
public class RoomMemberController {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoomMemberRepository roomMemberRepository;

    public RoomMemberController(RoomRepository roomRepository,
                                PlayerRepository playerRepository,
                                RoomMemberRepository roomMemberRepository) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    @GetMapping
    public List<RoomMemberResponse> listMembers(@PathVariable Long roomId) {
        return roomMemberRepository.findByRoomId(roomId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public RoomMemberResponse join(@PathVariable Long roomId, @RequestBody JoinRoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        RoomMember member = roomMemberRepository
                .findByRoomIdAndPlayerId(roomId, request.getPlayerId())
                .orElse(null);

        if (member == null) {
            member = new RoomMember();
            member.setRoom(room);
            member.setPlayer(player);
            member = roomMemberRepository.save(member);
        }

        return toResponse(member);
    }

    @DeleteMapping("/{playerId}")
    public void leave(@PathVariable Long roomId, @PathVariable Long playerId) {
        RoomMember member = roomMemberRepository
                .findByRoomIdAndPlayerId(roomId, playerId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        roomMemberRepository.delete(member);
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
}