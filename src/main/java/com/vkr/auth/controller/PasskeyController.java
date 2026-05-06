package com.vkr.auth.controller;

import com.vkr.auth.cache.FailedAttemptsCache;
import com.vkr.auth.config.InMemoryCredentialRepository;
import com.vkr.auth.dto.AuthResponse;
import com.vkr.auth.dto.ErrorResponse;
import com.vkr.auth.model.*;
import com.vkr.auth.repository.RefreshTokenRepository;
import com.vkr.auth.service.*;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.UserIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth/passkey")
@RequiredArgsConstructor
@Slf4j
public class PasskeyController {

    private final InMemoryCredentialRepository credentialRepository;
    private final WebAuthnService webAuthnService;
    private final UserService userService;
    private final AuthLogService authLogService;
    private final WebAuthnCredentialService webAuthnCredentialService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FailedAttemptsCache failedAttemptsCache;
    private User getOrCreateUser(String username) {
        Optional<User> existingUser = userService.findByUsername(username);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash("");
        newUser.setRole(Role.USER);
        newUser.setBlocked(false);
        return userService.save(newUser);
    }

    @PostMapping("/register/start")
    public ResponseEntity<?> startRegistration(@RequestParam String username, HttpSession session,
                                               HttpServletRequest request) {
        try {
            User user = getOrCreateUser(username);
            if (user.isBlocked()) {
                authLogService.logAuthAttempt(username, "PASSKEY_REGISTER", request, false, "Account is blocked", null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("account_blocked", "Account is blocked. Cannot register new passkeys.", 403));
            }
            PublicKeyCredentialCreationOptions options = webAuthnService.startRegistration(user);
            session.setAttribute("currentRegistrationOptions", options);
            session.setAttribute("registrationUsername", username);
            String json = options.toCredentialsCreateJson();
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            log.error("Start registration error", e);
            return ResponseEntity.badRequest().body(new ErrorResponse("registration_start_failed", e.getMessage(), 400));
        }
    }

    @PostMapping("/register/finish")
    public ResponseEntity<?> finishRegistration(@RequestBody String credentialJson, HttpSession session,
                                                HttpServletRequest request) {
        try {
            PublicKeyCredentialCreationOptions options = (PublicKeyCredentialCreationOptions)
                    session.getAttribute("currentRegistrationOptions");
            if (options == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("no_options", "No registration options found", 400));
            }
            RegistrationResult result = webAuthnService.finishRegistration(options, credentialJson);
            String username = (String) session.getAttribute("registrationUsername");
            User user = userService.findByUsernameOrThrow(username);

            if (user.isBlocked()) {
                authLogService.logAuthAttempt(username, "PASSKEY_REGISTER", request, false, "Account is blocked", null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("account_blocked", "Account is blocked. Registration aborted.", 403));
            }

            String credentialId = result.getKeyId().getId().getBase64Url();
            if (webAuthnCredentialService.existsByCredentialId(credentialId)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("already_registered",
                                "This authenticator is already registered for this user.", 400));
            }

            WebAuthnCredential credential = new WebAuthnCredential();
            credential.setCredentialId(credentialId);
            credential.setUser(user);
            credential.setCounter(0);
            credential.setPublicKeyCose(result.getPublicKeyCose().getBytes());
            webAuthnCredentialService.save(credential);

            UserIdentity userIdentity = UserIdentity.builder()
                    .name(username)
                    .displayName(username)
                    .id(new ByteArray(user.getId().getBytes(StandardCharsets.UTF_8)))
                    .build();
            RegisteredCredential registered = RegisteredCredential.builder()
                    .credentialId(result.getKeyId().getId())
                    .userHandle(userIdentity.getId())
                    .publicKeyCose(result.getPublicKeyCose())
                    .signatureCount(0)
                    .build();
            credentialRepository.addRegistration(registered, userIdentity);

            authLogService.logAuthAttempt(username, "PASSKEY_REGISTER", request, true, null, null);
            return ResponseEntity.ok(Map.of("success", true, "credentialId", credential.getCredentialId()));
        } catch (Exception e) {
            log.error("Finish registration error", e);
            authLogService.logAuthAttempt(null, "PASSKEY_REGISTER", request, false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("registration_failed", e.getMessage(), 500));
        }
    }

    @PostMapping("/login/start")
    public ResponseEntity<?> startLogin(@RequestParam String username, HttpSession session,
                                        HttpServletRequest request) {
        try {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (user.isBlocked()) {
                authLogService.logAuthAttempt(username, "PASSKEY", request, false, "Account is blocked", null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("account_blocked", "Account is blocked.", 403));
            }
            AssertionRequest req = webAuthnService.startLogin(user);
            session.setAttribute("currentAssertionRequest", req);
            session.setAttribute("loginUsername", username);
            String json = req.toJson();
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            log.error("Start login error", e);
            return ResponseEntity.badRequest().body(new ErrorResponse("login_start_failed", e.getMessage(), 400));
        }
    }

    @PostMapping("/login/finish")
    public ResponseEntity<?> finishLogin(@RequestBody String assertionJson, HttpSession session,
                                         HttpServletRequest request) {
        try {
            AssertionRequest assertionRequest = (AssertionRequest) session.getAttribute("currentAssertionRequest");
            if (assertionRequest == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("no_options", "No assertion options found", 400));
            }
            AssertionResult result = webAuthnService.finishLogin(assertionRequest, assertionJson);
            boolean success = result.isSuccess();
            String username = (String) session.getAttribute("loginUsername");
            if (username == null && result.getUsername() != null) {
                username = result.getUsername();
            }
            if (success) {
                // --- ПРОВЕРЯЕМ ДВУХФАКТОРНЫЙ КОНТЕКСТ (MFA) ---
                User mfaUser = (User) session.getAttribute("pendingMfaUser");
                if (mfaUser != null) {
                    // Это завершение двухфакторной аутентификации (Face + Passkey)

                    Double mfaConfidence = (Double) session.getAttribute("pendingMfaConfidence");
                    session.removeAttribute("pendingMfaUser");
                    session.removeAttribute("pendingMfaConfidence");

                    // Логируем двухфакторный вход
                    String method = "FACE+PASSKEY";
                    authLogService.logAuthAttempt(mfaUser.getUsername(), method, request, true, null,
                            mfaConfidence != null ? mfaConfidence : 0.0);
                    failedAttemptsCache.reset(mfaUser.getUsername());

                    // Генерируем JWT
                    String accessToken = jwtService.generateAccessToken(mfaUser.getUsername(), mfaUser.getRole().name());
                    String refreshToken = jwtService.generateRefreshToken(mfaUser.getUsername());

                    RefreshToken refreshEntity = new RefreshToken();
                    refreshEntity.setToken(refreshToken);
                    refreshEntity.setUsername(mfaUser.getUsername());
                    refreshEntity.setExpiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));
                    refreshEntity.setRevoked(false);
                    refreshTokenRepository.save(refreshEntity);

                    return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, "Authenticated with Face + Passkey"));
                }

                // Обычный вход по Passkey (без MFA)
                User user = userService.findByUsernameOrThrow(username);
                if (user.isBlocked()) {
                    authLogService.logAuthAttempt(username, "PASSKEY", request, false, "Account is blocked", null);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new ErrorResponse("account_blocked", "Ваша учётная запись заблокирована.", 403));
                }
                if (result.getSignatureCount() > 0) {
                    String base64Key = result.getCredentialId().getBase64Url();
                    webAuthnCredentialService.findByCredentialId(base64Key).ifPresent(cred -> {
                        cred.setCounter(result.getSignatureCount());
                        webAuthnCredentialService.save(cred);
                    });
                }

                String accessToken = jwtService.generateAccessToken(username, user.getRole().name());
                String refreshToken = jwtService.generateRefreshToken(username);

                RefreshToken refreshEntity = new RefreshToken();
                refreshEntity.setToken(refreshToken);
                refreshEntity.setUsername(username);
                refreshEntity.setExpiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));
                refreshEntity.setRevoked(false);
                refreshTokenRepository.save(refreshEntity);

                authLogService.logAuthAttempt(username, "PASSKEY", request, true, null, null);
                return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, "Authenticated with passkey"));
            } else {
                authLogService.logAuthAttempt(username, "PASSKEY", request, false, "Invalid assertion", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("auth_failed", "Passkey authentication failed", 401));
            }
        } catch (Exception e) {
            log.error("Finish login error", e);
            authLogService.logAuthAttempt(null, "PASSKEY", request, false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("auth_error", e.getMessage(), 500));
        }
    }
}