package com.vkr.auth.controller;

import com.vkr.auth.dto.AuthResponse;
import com.vkr.auth.cache.FailedAttemptsCache;
import com.vkr.auth.dto.ErrorResponse;
import com.vkr.auth.dto.FaceAuthenticationResult;
import com.vkr.auth.model.Role;
import com.vkr.auth.model.User;
import com.vkr.auth.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth/face")
@RequiredArgsConstructor
@Slf4j
public class FaceController {

    private final FaceRecognitionService faceService;
    private final UserService userService;
    private final AuthLogService authLogService;
    private final DynamicThresholdService thresholdService;
    private final FailedAttemptsCache failedAttemptsCache;

    @PostMapping("/register")
    public ResponseEntity<?> registerFace(@RequestParam String username,
                                          @RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        try {
            User user = userService.findByUsername(username).orElse(null);
            if (user == null) {
                user = new User();
                user.setUsername(username);
                user.setPasswordHash(""); // или зашифрованный временный пароль
                user.setRole(Role.USER);
                user.setBlocked(false);
                user = userService.save(user);
                log.info("Created new user: {}", username);
            }
            faceService.registerFace(user, file.getBytes());
            authLogService.logAuthAttempt(username, "FACE_REGISTER", request, true, null, null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Face registered successfully"));
        } catch (Exception e) {
            log.error("Face registration error", e);
            authLogService.logAuthAttempt(username, "FACE_REGISTER", request, false, e.getMessage(), null);
            return ResponseEntity.badRequest().body(new ErrorResponse("registration_failed", e.getMessage(), 400));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginFace(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) {
        try {
            Optional<FaceAuthenticationResult> resultOpt = faceService.authenticate(file.getBytes());
            if (resultOpt.isEmpty()) {
                authLogService.logAuthAttempt(null, "FACE", request, false, "No face detected", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("auth_failed", "No face detected or not recognized", 401));
            }

            FaceAuthenticationResult result = resultOpt.get();
            User user = result.getUser();
            double confidence = result.getConfidence();

            int failedAttempts = failedAttemptsCache.getFailedAttempts(user.getUsername());
            double threshold = thresholdService.computeThreshold(failedAttempts);
            boolean success = confidence >= threshold;

            if (success) {
                String token = "dummy-jwt-token-for-" + user.getUsername();
                authLogService.logAuthAttempt(user.getUsername(), "FACE", request, true, null, confidence);
                failedAttemptsCache.reset(user.getUsername());
                return ResponseEntity.ok(new AuthResponse(token, "Authenticated with face"));
            } else {
                String reason = String.format("Confidence %.2f < threshold %.2f", confidence, threshold);
                authLogService.logAuthAttempt(user.getUsername(), "FACE", request, false, reason, confidence);
                failedAttemptsCache.increment(user.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("auth_failed", reason, 401));
            }
        } catch (Exception e) {
            log.error("Face authentication error", e);
            authLogService.logAuthAttempt(null, "FACE", request, false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("auth_error", e.getMessage(), 500));
        }
    }
}