package com.example.trivzserver.service;

import com.example.trivzserver.entity.Player;
import com.example.trivzserver.repository.PlayerRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final PlayerRepository playerRepository;

    public CustomUserDetailsService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String role = player.getRole() == null || player.getRole().isBlank() ? "USER" : player.getRole();
        List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

        return new User(player.getUsername(), player.getPasswordHash(), auths);
    }
}
