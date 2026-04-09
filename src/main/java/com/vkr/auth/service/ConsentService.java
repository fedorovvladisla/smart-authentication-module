package com.vkr.auth.service;

import com.vkr.auth.model.UserConsent;
import com.vkr.auth.repository.ConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private final ConsentRepository consentRepository;

    @Transactional
    public void recordConsent(String userId, String consentVersion, HttpServletRequest request) {
        UserConsent consent = new UserConsent();
        consent.setUserId(userId);
        consent.setConsentTimestamp(LocalDateTime.now());
        consent.setConsentVersion(consentVersion);
        consent.setIpAddress(request.getRemoteAddr());
        consent.setUserAgent(request.getHeader("User-Agent"));
        consentRepository.save(consent);
        log.info("User {} consented to data processing (version {})", userId, consentVersion);
    }

    @Transactional(readOnly = true)
    public boolean hasConsented(String userId) {
        return consentRepository.existsByUserId(userId); // новый метод
    }

    @Transactional(readOnly = true)
    public Optional<UserConsent> findByUserId(String userId) {
        return consentRepository.findByUserId(userId);
    }

    @Transactional
    public void revokeConsent(String userId) {
        consentRepository.deleteById(userId);
        log.info("User {} revoked consent", userId);
    }
}