package com.vkr.auth.service;

import com.vkr.auth.model.User;
import com.vkr.auth.model.WebAuthnCredential;
import com.vkr.auth.repository.WebAuthnCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebAuthnCredentialService {

    private final WebAuthnCredentialRepository credentialRepository;

    @Transactional(readOnly = true)
    public List<WebAuthnCredential> findByUser(User user) {
        return credentialRepository.findByUser(user);
    }

    @Transactional
    public void save(WebAuthnCredential credential) {
        credentialRepository.save(credential);
        log.debug("Saved WebAuthn credential for user: {}", credential.getUser().getUsername());
    }

    @Transactional
    public void deleteByUser(User user) {
        credentialRepository.deleteByUser(user);
        log.info("Deleted all WebAuthn credentials for user: {}", user.getUsername());
    }

    @Transactional(readOnly = true)
    public Optional<WebAuthnCredential> findByCredentialId(String credentialId) {
        return credentialRepository.findByCredentialId(credentialId);
    }

    @Transactional(readOnly = true)
    public boolean existsByCredentialId(String credentialId) {
        return credentialRepository.existsByCredentialId(credentialId); // новый метод
    }

    @Transactional
    public void deleteByCredentialId(String credentialId) {
        credentialRepository.deleteById(credentialId);
        log.info("Deleted WebAuthn credential: {}", credentialId);
    }
}