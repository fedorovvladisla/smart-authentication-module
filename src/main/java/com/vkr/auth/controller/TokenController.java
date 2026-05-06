package com.vkr.auth.controller;

import com.vkr.auth.dto.AuthResponse;
import com.vkr.auth.model.RefreshToken;
import com.vkr.auth.repository.RefreshTokenRepository;
import com.vkr.auth.service.JwtService;
import com.vkr.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth/token")
@RequiredArgsConstructor
public class TokenController {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required"));
        }

        try {
            jwtService.validateRefreshToken(refreshToken);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }

        Optional<RefreshToken> optToken = refreshTokenRepository.findByToken(refreshToken);
        if (optToken.isEmpty() || optToken.get().isRevoked() || optToken.get().getExpiryDate().isBefore(Instant.now())) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token expired or revoked"));
        }

        RefreshToken storedToken = optToken.get();
        String username = storedToken.getUsername();

        // Получаем роль пользователя
        String role = userService.findByUsernameOrThrow(username).getRole().name();

        // Генерируем новую пару
        String newAccessToken = jwtService.generateAccessToken(username, role);
        String newRefreshToken = jwtService.generateRefreshToken(username);

        // Отзываем старый refresh
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Сохраняем новый
        RefreshToken newToken = new RefreshToken();
        newToken.setToken(newRefreshToken);
        newToken.setUsername(username);
        newToken.setExpiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));
        newToken.setRevoked(false);
        refreshTokenRepository.save(newToken);

        return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken, "Tokens refreshed"));
    }
}