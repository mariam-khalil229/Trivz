package com.example.trivzserver.controller;

import com.example.trivzserver.dto.PlayerRequest;
import com.example.trivzserver.dto.PlayerResponse;
import com.example.trivzserver.entity.Player;
import com.example.trivzserver.repository.PlayerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
        player.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        player.setCreatedAt(java.time.LocalDateTime.now());

        // Bootstrap: very first registered user becomes ADMIN. Everyone else is USER.
        long existing = playerRepository.count();
        player.setRole(existing == 0 ? "ADMIN" : "USER");

        Player saved = playerRepository.save(player);
        return ResponseEntity.ok(PlayerResponse.fromEntity(saved));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public PlayerResponse updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        if (newRole == null || (!newRole.equalsIgnoreCase("ADMIN") && !newRole.equalsIgnoreCase("USER"))) {
            throw new RuntimeException("role must be ADMIN or USER");
        }
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        player.setRole(newRole.toUpperCase());
        return PlayerResponse.fromEntity(playerRepository.save(player));
    }
}
