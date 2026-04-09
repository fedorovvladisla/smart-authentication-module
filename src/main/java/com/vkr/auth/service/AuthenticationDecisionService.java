package com.vkr.auth.service;

import com.vkr.auth.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationDecisionService {

    private final UserService userService;
    private final WebAuthnCredentialService webAuthnCredentialService;

    public String decideMethod(String username) {
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return "PASSWORD";
        }

        boolean hasPasskey = !webAuthnCredentialService.findByUser(user).isEmpty();
        if (hasPasskey) {
            return "PASSKEY";
        }

        return "PASSWORD";
    }
}