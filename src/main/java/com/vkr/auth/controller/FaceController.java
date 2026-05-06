package com.vkr.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vkr.auth.cache.FailedAttemptsCache;
import com.vkr.auth.dto.AuthResponse;
import com.vkr.auth.dto.ErrorResponse;
import com.vkr.auth.dto.FaceAuthenticationResult;
import com.vkr.auth.model.RefreshToken;
import com.vkr.auth.model.Role;
import com.vkr.auth.model.User;
import com.vkr.auth.repository.RefreshTokenRepository;
import com.vkr.auth.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
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
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${compreface.base-url}")
    private String comprefaceBaseUrl;

    @Value("${compreface.api-key}")
    private String comprefaceApiKey;

    // ----- вспомогательный класс для хранения результата распознавания с позой -----
    private static class RecognitionResult {
        String subject;
        double confidence;
        double[] pose; // [yaw, pitch, roll]

        public RecognitionResult(String subject, double confidence, double[] pose) {
            this.subject = subject;
            this.confidence = confidence;
            this.pose = pose;
        }
    }

    // ----- вызов распознавания с face_plugins=pose -----
    private RecognitionResult recognizeWithPose(byte[] imageBytes) {
        try {
            String url = comprefaceBaseUrl + "/api/v1/recognition/recognize?face_plugins=pose";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("x-api-key", comprefaceApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "frame.jpg";
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            log.debug("Recognize with pose response: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode resultArray = root.path("result");
            if (resultArray.isEmpty()) {
                return null;
            }
            JsonNode firstResult = resultArray.get(0);
            JsonNode subjects = firstResult.path("subjects");
            String subject = null;
            double confidence = 0.0;
            if (!subjects.isEmpty()) {
                JsonNode topSubject = subjects.get(0);
                subject = topSubject.path("subject").asText();
                confidence = topSubject.path("similarity").asDouble();
            }
            // извлекаем углы
            JsonNode poseNode = firstResult.path("pose");
            double[] angles = null;
            if (!poseNode.isMissingNode()) {
                double yaw = poseNode.path("yaw").asDouble();
                double pitch = poseNode.path("pitch").asDouble();
                double roll = poseNode.path("roll").asDouble();
                angles = new double[]{yaw, pitch, roll};
            }
            return new RecognitionResult(subject, confidence, angles);
        } catch (Exception e) {
            log.error("Recognize with pose error", e);
            return null;
        }
    }

    // ----- обычная регистрация лица (без проверки живости, но с проверкой уникальности) -----
    @PostMapping("/register")
    public ResponseEntity<?> registerFace(@RequestParam String username,
                                          @RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        try {
            // 1. Проверка уникальности лица (до создания пользователя)
            log.debug("Checking face uniqueness for user {}", username);
            Optional<FaceAuthenticationResult> existing = faceService.authenticate(file.getBytes());
            if (existing.isPresent() && existing.get().getConfidence() > 0.9) {
                String existingUsername = existing.get().getUser().getUsername();
                log.warn("Face already registered as user: {}, confidence: {}", existingUsername, existing.get().getConfidence());
                authLogService.logAuthAttempt(username, "FACE_REGISTER", request, false,
                        "Face already registered as user: " + existingUsername, existing.get().getConfidence());
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("duplicate_face",
                                "This face is already registered under username: " + existingUsername, 400));
            }

            // 2. Создаём или находим пользователя
            User user = userService.findByUsername(username).orElse(null);
            if (user == null) {
                user = new User();
                user.setUsername(username);
                user.setPasswordHash("");
                user.setRole(Role.USER);
                user.setBlocked(false);
                user = userService.save(user);
                log.info("Created new user: {}", username);
            } else if (user.isBlocked()) {
                authLogService.logAuthAttempt(username, "FACE_REGISTER", request, false, "Account is blocked", null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("account_blocked", "Account is blocked.", 403));
            }

            // 3. Сохраняем лицо в CompreFace
            faceService.registerFace(user, file.getBytes());
            authLogService.logAuthAttempt(username, "FACE_REGISTER", request, true, null, null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Face registered successfully"));
        } catch (Exception e) {
            log.error("Face registration error", e);
            authLogService.logAuthAttempt(username, "FACE_REGISTER", request, false, e.getMessage(), null);
            return ResponseEntity.badRequest().body(new ErrorResponse("registration_failed", e.getMessage(), 400));
        }
    }

    // ----- регистрация с проверкой живости (два кадра) -----
    @PostMapping("/register-liveness")
    public ResponseEntity<?> registerWithLiveness(@RequestParam String username,
                                                  @RequestParam("frame1") MultipartFile frame1,
                                                  @RequestParam("frame2") MultipartFile frame2,
                                                  HttpServletRequest request) {
        try {
            // 1. Проверка уникальности + живость (пользователь ещё не создаётся)
            RecognitionResult result1 = recognizeWithPose(frame1.getBytes());
            if (result1 == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("face_not_detected", "Could not detect face in frame1", 400));
            }
            if (result1.subject != null && result1.confidence > 0.9) {
                log.warn("Face already registered during liveness: existing user={}, confidence={}",
                        result1.subject, result1.confidence);
                authLogService.logAuthAttempt(username, "FACE_REGISTER", request, false,
                        "Face already registered as user: " + result1.subject, result1.confidence);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("duplicate_face",
                                "Face already registered under username: " + result1.subject, 400));
            }

            RecognitionResult result2 = recognizeWithPose(frame2.getBytes());
            if (result2 == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("face_not_detected", "Could not detect face in frame2", 400));
            }
            if (result1.pose == null || result2.pose == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("pose_failed", "Could not obtain pose angles", 400));
            }

            double[] angles1 = result1.pose;
            double[] angles2 = result2.pose;
            log.debug("Angles frame1: {}", java.util.Arrays.toString(angles1));
            log.debug("Angles frame2: {}", java.util.Arrays.toString(angles2));

            boolean moved = Math.abs(angles1[0] - angles2[0]) > 10 ||
                    Math.abs(angles1[1] - angles2[1]) > 10 ||
                    Math.abs(angles1[2] - angles2[2]) > 10;

            if (!moved) {
                authLogService.logAuthAttempt(username, "FACE_REGISTER", request, false, "Liveness check failed", null);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("liveness_failed", "Please move your head. No significant motion detected.", 400));
            }

            // 2. Все проверки пройдены – создаём пользователя
            User user = userService.findByUsername(username).orElse(null);
            if (user == null) {
                user = new User();
                user.setUsername(username);
                user.setPasswordHash("");
                user.setRole(Role.USER);
                user.setBlocked(false);
                user = userService.save(user);
                log.info("Created new user: {}", username);
            } else if (user.isBlocked()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("account_blocked", "Account is blocked.", 403));
            }

            // 3. Сохраняем лицо
            faceService.registerFace(user, frame1.getBytes());
            authLogService.logAuthAttempt(username, "FACE_REGISTER", request, true, null, null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Face registered with liveness check"));
        } catch (Exception e) {
            log.error("Liveness registration error for user {}", username, e);
            authLogService.logAuthAttempt(username, "FACE_REGISTER", request, false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("registration_failed", e.getMessage(), 500));
        }
    }

    // ----- вход по лицу (с двухфакторной логикой) -----
    @PostMapping("/login")
    public ResponseEntity<?> loginFace(@RequestParam("file") MultipartFile file,
                                       @RequestParam("username") String username,
                                       HttpServletRequest request,
                                       HttpSession session) {
        try {
            // 1. Распознаём лицо
            RecognitionResult recognition = recognizeWithPose(file.getBytes());
            if (recognition == null || recognition.subject == null) {
                authLogService.logAuthAttempt(username, "FACE", request, false, "No face detected", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("auth_failed", "No face detected or not recognized", 401));
            }

            // 2. Ищем пользователя по subjectId
            Optional<User> userOpt = userService.findById(recognition.subject);
            if (userOpt.isEmpty()) {
                authLogService.logAuthAttempt(username, "FACE", request, false,
                        "Face belongs to deleted user: " + recognition.subject, recognition.confidence);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("user_not_found",
                                "Это лицо принадлежит пользователю, которого больше нет в системе.", 403));
            }

            User user = userOpt.get();
            double confidence = recognition.confidence;

            // 3. Проверка: совпадает ли введённое имя с тем, кого узнали
            if (!user.getUsername().equalsIgnoreCase(username)) {
                authLogService.logAuthAttempt(username, "FACE", request, false,
                        "Face belongs to another user: " + user.getUsername(), confidence);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("wrong_user",
                                "Это лицо принадлежит пользователю " + user.getUsername() + ", а не " + username, 403));
            }

            // 4. Проверка блокировки
            if (user.isBlocked()) {
                authLogService.logAuthAttempt(username, "FACE", request, false, "Account is blocked", null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("account_blocked", "Account is blocked.", 403));
            }

            // 5. Динамический порог
            int failedAttempts = failedAttemptsCache.getFailedAttempts(user.getUsername());
            double threshold = thresholdService.computeThreshold(failedAttempts);
            boolean success = confidence >= threshold;

            if (success) {
                final double HIGH_CONFIDENCE = 0.95;

                if (confidence >= HIGH_CONFIDENCE) {
                    // Сильная уверенность – сразу аутентифицируем
                    return issueTokensAndResponse(user, confidence, request);
                } else {
                    // Средняя уверенность – запрашиваем Passkey
                    session.setAttribute("pendingMfaUser", user);
                    session.setAttribute("pendingMfaConfidence", confidence);
                    return ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(new ErrorResponse("mfa_required",
                                    "Face recognized, but confidence is moderate. Please confirm with your Passkey.", 202));
                }
            } else {
                String reason = String.format("Confidence %.2f < threshold %.2f", confidence, threshold);
                authLogService.logAuthAttempt(username, "FACE", request, false, reason, confidence);
                failedAttemptsCache.increment(username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("auth_failed", reason, 401));
            }
        } catch (Exception e) {
            log.error("Face authentication error", e);
            authLogService.logAuthAttempt(username, "FACE", request, false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("auth_error", e.getMessage(), 500));
        }
    }

    private ResponseEntity<AuthResponse> issueTokensAndResponse(User user, double confidence, HttpServletRequest request) {
        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        RefreshToken refreshEntity = new RefreshToken();
        refreshEntity.setToken(refreshToken);
        refreshEntity.setUsername(user.getUsername());
        refreshEntity.setExpiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));
        refreshEntity.setRevoked(false);
        refreshTokenRepository.save(refreshEntity);

        authLogService.logAuthAttempt(user.getUsername(), "FACE", request, true, null, confidence);
        failedAttemptsCache.reset(user.getUsername());
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, "Authenticated with face"));
    }
}