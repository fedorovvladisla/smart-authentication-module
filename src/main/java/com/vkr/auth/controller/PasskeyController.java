package com.vkr.auth.controller;

import com.vkr.auth.dto.AuthResponse;
import com.vkr.auth.dto.ErrorResponse;
import com.vkr.auth.model.User;
import com.vkr.auth.service.AuthLogService;
import com.vkr.auth.service.UserService;
import com.vkr.auth.service.WebAuthnService;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.RegistrationResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/passkey")
@RequiredArgsConstructor
@Slf4j
public class PasskeyController {

    private final WebAuthnService webAuthnService;
    private final UserService userService;
    private final AuthLogService authLogService;

    @PostMapping("/register/start")
    public ResponseEntity<?> startRegistration(@RequestParam String username, HttpSession session) {
        try {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            PublicKeyCredentialCreationOptions options = webAuthnService.startRegistration(user);
            session.setAttribute("currentRegistrationOptions", options);
            session.setAttribute("registrationUsername", username);
            return ResponseEntity.ok(options);
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
            authLogService.logAuthAttempt(username, "PASSKEY_REGISTER", request, true, null, null);
            return ResponseEntity.ok(Map.of("success", true, "credentialId", result.getKeyId().getId().getBase64Url()));
        } catch (Exception e) {
            log.error("Finish registration error", e);
            authLogService.logAuthAttempt(null, "PASSKEY_REGISTER", request, false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("registration_failed", e.getMessage(), 500));
        }
    }

    @PostMapping("/login/start")
    public ResponseEntity<?> startLogin(@RequestParam String username, HttpSession session) {
        try {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            AssertionRequest request = webAuthnService.startLogin(user);
            session.setAttribute("currentAssertionRequest", request);
            session.setAttribute("loginUsername", username);
            return ResponseEntity.ok(request);
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
            var result = webAuthnService.finishLogin(assertionRequest, assertionJson);
            boolean success = result.isSuccess();
            String username = (String) session.getAttribute("loginUsername");
            if (username == null && result.getUsername() != null) {
                username = result.getUsername();
            }
            if (success) {
                String token = "dummy-jwt-token-for-" + username;
                authLogService.logAuthAttempt(username, "PASSKEY", request, true, null, null);
                return ResponseEntity.ok(new AuthResponse(token, "Authenticated with passkey"));
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