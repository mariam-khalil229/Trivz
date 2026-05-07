package com.example.trivzserver.controller;

import com.example.trivzserver.dto.AuthRequest;
import com.example.trivzserver.dto.AuthResponse;
import com.example.trivzserver.entity.Player;
import com.example.trivzserver.repository.PlayerRepository;
import com.example.trivzserver.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(PlayerRepository playerRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        Player player = playerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), player.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        player.getUsername(),
                        player.getPasswordHash(),
                        java.util.Collections.emptyList()
                )
        );

        return ResponseEntity.ok(new AuthResponse(token));
    }
}