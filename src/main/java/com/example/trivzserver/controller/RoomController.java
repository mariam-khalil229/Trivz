package com.example.trivzserver.controller;

import com.example.trivzserver.dto.RoomRequest;
import com.example.trivzserver.dto.RoomResponse;
import com.example.trivzserver.entity.Room;
import com.example.trivzserver.repository.RoomRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final Random random = new Random();

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
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
        Room room = new Room();
        room.setName(request.getName());
        room.setMaxPlayers(request.getMaxPlayers() != null ? request.getMaxPlayers() : 8);
        room.setStatus("LOBBY");
        room.setCode(generateUniqueCode());

        Room saved = roomRepository.save(room);
        return toResponse(saved);
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
        return new RoomResponse(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getMaxPlayers(),
                room.getStatus(),
                room.getCreatedAt()
        );
    }
}
