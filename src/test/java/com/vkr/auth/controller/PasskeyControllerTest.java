package com.vkr.auth.controller;

import com.vkr.auth.cache.FailedAttemptsCache;
import com.vkr.auth.config.InMemoryCredentialRepository;
import com.vkr.auth.dto.AuthResponse;
import com.vkr.auth.dto.ErrorResponse;
import com.vkr.auth.model.Role;
import com.vkr.auth.model.User;
import com.vkr.auth.repository.RefreshTokenRepository;
import com.vkr.auth.service.*;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PasskeyControllerTest {

    private MockMvc mockMvc;
    private final InMemoryCredentialRepository credentialRepository = mock(InMemoryCredentialRepository.class);
    private final WebAuthnService webAuthnService = mock(WebAuthnService.class);
    private final UserService userService = mock(UserService.class);
    private final AuthLogService authLogService = mock(AuthLogService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final WebAuthnCredentialService webAuthnCredentialService = mock(WebAuthnCredentialService.class);
    private final FailedAttemptsCache failedAttemptsCache = mock(FailedAttemptsCache.class);

    private User testUser;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash("");
        testUser.setRole(Role.USER);
        testUser.setBlocked(false);

        session = new MockHttpSession();
        session.setMaxInactiveInterval(300);

        PasskeyController controller = new PasskeyController(
                credentialRepository,
                webAuthnService,
                userService,
                authLogService,
                webAuthnCredentialService,
                jwtService,
                refreshTokenRepository,
                failedAttemptsCache
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void startLogin_ShouldReturnAssertionOptions() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        AssertionRequest req = AssertionRequest.builder()
                .publicKeyCredentialRequestOptions(PublicKeyCredentialRequestOptions.builder()
                        .challenge(new ByteArray(new byte[32]))
                        .build())
                .build();
        when(webAuthnService.startLogin(testUser)).thenReturn(req);

        mockMvc.perform(post("/api/auth/passkey/login/start")
                        .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void startLogin_ShouldReturn403_WhenUserBlocked() throws Exception {
        testUser.setBlocked(true);
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/passkey/login/start")
                        .param("username", "testuser"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("account_blocked"));
    }

    @Test
    void finishLogin_ShouldReturnTokens() throws Exception {
        // Arrange
        AssertionRequest assertionRequest = AssertionRequest.builder()
                .publicKeyCredentialRequestOptions(PublicKeyCredentialRequestOptions.builder()
                        .challenge(new ByteArray(new byte[32]))
                        .build())
                .build();
        session.setAttribute("currentAssertionRequest", assertionRequest);
        session.setAttribute("loginUsername", "testuser");

        AssertionResult assertionResult = mock(AssertionResult.class);
        when(assertionResult.isSuccess()).thenReturn(true);
        when(assertionResult.getUsername()).thenReturn("testuser");
        when(webAuthnService.finishLogin(eq(assertionRequest), anyString())).thenReturn(assertionResult);

        when(userService.findByUsernameOrThrow("testuser")).thenReturn(testUser);
        when(jwtService.generateAccessToken("testuser", "USER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("testuser")).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenReturn(null);

        mockMvc.perform(post("/api/auth/passkey/login/finish")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.message").value("Authenticated with passkey"));
    }

    @Test
    void finishLogin_ShouldReturn403_WhenUserBlocked() throws Exception {
        testUser.setBlocked(true);
        AssertionRequest assertionRequest = AssertionRequest.builder()
                .publicKeyCredentialRequestOptions(PublicKeyCredentialRequestOptions.builder()
                        .challenge(new ByteArray(new byte[32]))
                        .build())
                .build();
        session.setAttribute("currentAssertionRequest", assertionRequest);
        session.setAttribute("loginUsername", "testuser");

        AssertionResult assertionResult = mock(AssertionResult.class);
        when(assertionResult.isSuccess()).thenReturn(true);
        when(webAuthnService.finishLogin(eq(assertionRequest), anyString())).thenReturn(assertionResult);
        when(userService.findByUsernameOrThrow("testuser")).thenReturn(testUser);

        mockMvc.perform(post("/api/auth/passkey/login/finish")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("account_blocked"));
    }

    @Test
    void finishLogin_ShouldCompleteMfa_WhenPendingUserPresent() throws Exception {
        // Arrange
        AssertionRequest assertionRequest = AssertionRequest.builder()
                .publicKeyCredentialRequestOptions(PublicKeyCredentialRequestOptions.builder()
                        .challenge(new ByteArray(new byte[32]))
                        .build())
                .build();
        session.setAttribute("currentAssertionRequest", assertionRequest);
        session.setAttribute("loginUsername", "mfa_user");
        session.setAttribute("pendingMfaUser", testUser);
        session.setAttribute("pendingMfaConfidence", 0.92);

        AssertionResult assertionResult = mock(AssertionResult.class);
        when(assertionResult.isSuccess()).thenReturn(true);
        when(webAuthnService.finishLogin(eq(assertionRequest), anyString())).thenReturn(assertionResult);

        when(jwtService.generateAccessToken("testuser", "USER")).thenReturn("mfa-access");
        when(jwtService.generateRefreshToken("testuser")).thenReturn("mfa-refresh");

        mockMvc.perform(post("/api/auth/passkey/login/finish")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mfa-access"))
                .andExpect(jsonPath("$.refreshToken").value("mfa-refresh"))
                .andExpect(jsonPath("$.message").value("Authenticated with Face + Passkey"));
    }
}