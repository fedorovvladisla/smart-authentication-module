package com.vkr.auth.controller;

import com.vkr.auth.dto.ConsentRequest;
import com.vkr.auth.dto.ErrorResponse;
import com.vkr.auth.service.ConsentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/consent")
@RequiredArgsConstructor
@Slf4j
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping
    public ResponseEntity<?> recordConsent(@RequestBody ConsentRequest consentRequest,
                                           HttpServletRequest request,
                                           Principal principal) {
        try {
            String userId = principal.getName(); // предполагаем, что пользователь аутентифицирован
            if (userId == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("unauthorized", "User not authenticated", 401));
            }
            if (!consentRequest.isAgreed()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("consent_required", "User must agree to data processing", 400));
            }
            consentService.recordConsent(userId, consentRequest.getConsentVersion(), request);
            return ResponseEntity.ok(Map.of("success", true, "message", "Consent recorded"));
        } catch (Exception e) {
            log.error("Consent recording error", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("consent_error", e.getMessage(), 500));
        }
    }
}