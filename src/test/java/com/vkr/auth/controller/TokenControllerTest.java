package com.vkr.auth.controller;

import com.vkr.auth.model.RefreshToken;
import com.vkr.auth.model.Role;
import com.vkr.auth.model.User;
import com.vkr.auth.repository.RefreshTokenRepository;
import com.vkr.auth.service.JwtService;
import com.vkr.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TokenControllerTest {

    private MockMvc mockMvc;
    private final JwtService jwtService = mock(JwtService.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final UserService userService = mock(UserService.class);

    @BeforeEach
    void setUp() {
        TokenController controller = new TokenController(jwtService, refreshTokenRepository, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void refresh_ShouldReturnNewTokens_WhenValidRefreshToken() throws Exception {
        String oldRefresh = "old-refresh";
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(oldRefresh);
        storedToken.setUsername("testuser");
        storedToken.setExpiryDate(Instant.now().plusSeconds(3600));
        storedToken.setRevoked(false);

        when(refreshTokenRepository.findByToken(oldRefresh)).thenReturn(Optional.of(storedToken));
        when(jwtService.validateRefreshToken(oldRefresh)).thenReturn(null); // успех
        when(userService.findByUsernameOrThrow("testuser")).thenReturn(createUser());
        when(jwtService.generateAccessToken("testuser", "USER")).thenReturn("new-access");
        when(jwtService.generateRefreshToken("testuser")).thenReturn("new-refresh");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(storedToken);

        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"))
                .andExpect(jsonPath("$.message").value("Tokens refreshed"));
    }

    @Test
    void refresh_ShouldReturn401_WhenTokenRevoked() throws Exception {
        String revokedToken = "revoked-token";
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(revokedToken);
        storedToken.setRevoked(true);
        storedToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken(revokedToken)).thenReturn(Optional.of(storedToken));
        when(jwtService.validateRefreshToken(revokedToken)).thenReturn(null);

        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + revokedToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Refresh token expired or revoked"));
    }

    private User createUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setRole(Role.USER);
        return user;
    }
}