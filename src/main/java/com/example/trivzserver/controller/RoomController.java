package com.example.trivzserver.controller;

import com.example.trivzserver.dto.RoomRequest;
import com.example.trivzserver.dto.RoomResponse;
import com.example.trivzserver.entity.Player;
import com.example.trivzserver.entity.Room;
import com.example.trivzserver.entity.RoomMember;
import com.example.trivzserver.repository.PlayerRepository;
import com.example.trivzserver.repository.RoomMemberRepository;
import com.example.trivzserver.repository.RoomRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final Random random = new Random();

    public RoomController(RoomRepository roomRepository,
                          PlayerRepository playerRepository,
                          RoomMemberRepository roomMemberRepository) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    @GetMapping
    public List<RoomResponse> getAll() {
        return roomRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public RoomResponse getById(@PathVariable Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return toResponse(room);
    }

    @GetMapping("/code/{code}")
    public RoomResponse getByCode(@PathVariable String code) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return toResponse(room);
    }

    @PostMapping
    public RoomResponse create(@RequestBody RoomRequest request) {
        Player host = currentPlayer();

        Room room = new Room();
        room.setName(request.getName());
        room.setMaxPlayers(request.getMaxPlayers() != null ? request.getMaxPlayers() : 8);
        room.setStatus("LOBBY");
        room.setCode(generateUniqueCode());
        room.setHostPlayerId(host.getId());
        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) {
            room.setDifficulty(request.getDifficulty().trim().toLowerCase());
        }

        Room saved = roomRepository.save(room);

        RoomMember creatorMember = new RoomMember();
        creatorMember.setRoom(saved);
        creatorMember.setPlayer(host);
        roomMemberRepository.save(creatorMember);

        return toResponse(saved);
    }

    private Player currentPlayer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Not authenticated");
        }
        return playerRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = randomCode(6);
        } while (roomRepository.findByCode(code).isPresent());
        return code;
    }

    private String randomCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private RoomResponse toResponse(Room room) {
        String hostUsername = null;
        if (room.getHostPlayerId() != null) {
            hostUsername = playerRepository.findById(room.getHostPlayerId())
                    .map(Player::getUsername)
                    .orElse(null);
        }
        long memberCount = roomMemberRepository.countByRoomId(room.getId());

        return new RoomResponse(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getMaxPlayers(),
                room.getStatus(),
                room.getHostPlayerId(),
                hostUsername,
                memberCount,
                room.getDifficulty(),
                room.getCreatedAt()
        );
    }
}
