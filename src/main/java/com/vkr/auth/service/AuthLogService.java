package com.vkr.auth.service;

import com.vkr.auth.model.AuthLog;
import com.vkr.auth.repository.AuthLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthLogService {

    private final AuthLogRepository authLogRepository;
    private final GeoIpService geoIpService;  // нужно создать этот сервис (см. ниже)

    @Transactional
    public void logAuthAttempt(String username, String method, HttpServletRequest request,
                               boolean success, String failureReason, Double confidence) {
        AuthLog logEntry = new AuthLog();  // переименовал, чтобы не конфликтовать с логгером
        logEntry.setUsername(username);
        logEntry.setTimestamp(LocalDateTime.now());
        logEntry.setMethod(method);
        logEntry.setSuccess(success);
        logEntry.setFailureReason(failureReason);
        logEntry.setConfidence(confidence != null ? confidence : 0.0);

        String ip = request.getRemoteAddr();
        logEntry.setIpAddress(ip);
        logEntry.setGeoLocation(geoIpService.getGeoLocation(ip));

        authLogRepository.save(logEntry);
        log.debug("Auth attempt logged: {} {} from {}", method, success ? "SUCCESS" : "FAILURE", ip);
    }

    @Transactional(readOnly = true)
    public Page<AuthLog> findAll(Pageable pageable) {
        return authLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuthLog> findByMethod(String method, Pageable pageable) {
        return authLogRepository.findByMethod(method, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuthLog> findBySuccess(boolean success, Pageable pageable) {
        return authLogRepository.findBySuccess(success, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuthLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return authLogRepository.findByTimestampBetween(from, to, pageable);
    }

    @Transactional(readOnly = true)
    public long countSuccess() {
        return authLogRepository.countBySuccess(true);
    }

    @Transactional(readOnly = true)
    public long countFailure() {
        return authLogRepository.countBySuccess(false);
    }
}