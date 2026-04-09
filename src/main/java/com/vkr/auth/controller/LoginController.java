package com.vkr.auth.controller;

import com.vkr.auth.dto.LoginInitResponse;
import com.vkr.auth.service.AuthenticationDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth/login")
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationDecisionService decisionService;

    @PostMapping("/init")
    public ResponseEntity<?> initLogin(@RequestParam String username) {
        String method = decisionService.decideMethod(username);
        return ResponseEntity.ok(new LoginInitResponse(method));
    }
}