package com.vkr.auth.config;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAuthnConfig {

    @Value("${auth.webauthn.rp-id:localhost}")
    private String rpId;

    @Value("${auth.webauthn.rp-name:Smart Authentication Module}")
    private String rpName;

    @Bean
    public InMemoryCredentialRepository credentialRepository() {
        return new InMemoryCredentialRepository();
    }

    @Bean
    public RelyingParty relyingParty(InMemoryCredentialRepository credentialRepository) {
        return RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id(rpId)
                        .name(rpName)
                        .build())
                .credentialRepository(credentialRepository)
                .allowOriginPort(true)
                .allowOriginSubdomain(true)
                .build();
    }
}