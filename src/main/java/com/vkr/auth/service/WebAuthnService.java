package com.vkr.auth.service;

import com.vkr.auth.model.User;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebAuthnService {

    private final RelyingParty relyingParty;

    public PublicKeyCredentialCreationOptions startRegistration(User user) {
        UserIdentity userIdentity = UserIdentity.builder()
                .name(user.getUsername())
                .displayName(user.getUsername())
                .id(new ByteArray(user.getId().getBytes(StandardCharsets.UTF_8)))
                .build();
        return relyingParty.startRegistration(
                StartRegistrationOptions.builder()
                        .user(userIdentity)
                        .build()
        );
    }

    public RegistrationResult finishRegistration(PublicKeyCredentialCreationOptions requestOptions,
                                                 String credentialJson) {
        try {
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> response =
                    PublicKeyCredential.parseRegistrationResponseJson(credentialJson);
            return relyingParty.finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(requestOptions)
                            .response(response)
                            .build()
            );
        } catch (RegistrationFailedException e) {
            log.error("WebAuthn registration failed", e);
            throw new RuntimeException("Registration failed", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AssertionRequest startLogin(User user) {
        return relyingParty.startAssertion(
                StartAssertionOptions.builder()
                        .username(user.getUsername())
                        .build()
        );
    }

    public AssertionResult finishLogin(AssertionRequest requestOptions, String assertionJson) {
        try {
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> response =
                    PublicKeyCredential.parseAssertionResponseJson(assertionJson);
            return relyingParty.finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(requestOptions)
                            .response(response)
                            .build()
            );
        } catch (AssertionFailedException e) {
            log.error("WebAuthn assertion failed", e);
            throw new RuntimeException("Authentication failed", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}