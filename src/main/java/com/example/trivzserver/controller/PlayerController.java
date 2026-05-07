package com.example.trivzserver.controller;

import com.example.trivzserver.dto.PlayerRequest;
import com.example.trivzserver.dto.PlayerResponse;
import com.example.trivzserver.entity.Player;
import com.example.trivzserver.repository.PlayerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    public PlayerController(PlayerRepository playerRepository, PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<PlayerResponse> getAllPlayers() {
        return playerRepository.findAll().stream()
                .map(PlayerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayer(@PathVariable Long id) {
        return playerRepository.findById(id)
                .map(PlayerResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(@RequestBody PlayerRequest request) {
        Player player = new Player();
        player.setUsername(request.getUsername());
        player.setDisplayName(request.getDisplayName());
        player.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        player.setCreatedAt(java.time.LocalDateTime.now());

        Player saved = playerRepository.save(player);
        return ResponseEntity.ok(PlayerResponse.fromEntity(saved));
    }
}