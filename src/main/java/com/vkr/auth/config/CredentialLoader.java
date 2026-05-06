package com.vkr.auth.config;

import com.vkr.auth.model.WebAuthnCredential;
import com.vkr.auth.repository.WebAuthnCredentialRepository;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.RegisteredCredential;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CredentialLoader {

    private final WebAuthnCredentialRepository webAuthnCredentialRepository;
    private final InMemoryCredentialRepository inMemoryCredentialRepository;

    @PostConstruct
    public void loadCredentialsFromDatabase() {
        List<WebAuthnCredential> credentials = webAuthnCredentialRepository.findAll();
        log.info("Loading {} WebAuthn credentials from DB into memory...", credentials.size());

        for (WebAuthnCredential cred : credentials) {
            try {
                UserIdentity userIdentity = UserIdentity.builder()
                        .name(cred.getUser().getUsername())
                        .displayName(cred.getUser().getUsername())
                        .id(new ByteArray(cred.getUser().getId().getBytes(StandardCharsets.UTF_8)))
                        .build();

                RegisteredCredential registered = RegisteredCredential.builder()
                        .credentialId(new ByteArray(Base64.getUrlDecoder().decode(cred.getCredentialId())))
                        .userHandle(new ByteArray(cred.getUser().getId().getBytes(StandardCharsets.UTF_8)))
                        .publicKeyCose(new ByteArray(cred.getPublicKeyCose()))
                        .signatureCount(cred.getCounter())
                        .build();

                inMemoryCredentialRepository.addRegistration(registered, userIdentity);
            } catch (Exception e) {
                log.error("Failed to load credential {}", cred.getCredentialId(), e);
            }
        }
    }
}