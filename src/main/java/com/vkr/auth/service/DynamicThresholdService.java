package com.vkr.auth.service;

import com.vkr.auth.model.AppSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicThresholdService {

    private final AppSettingsService settingsService;

    public double computeThreshold(int failedAttempts) {
        AppSettings settings = settingsService.getSettings();
        double base = settings.getBaseThreshold();
        double attemptPenalty = settings.getAttemptPenalty() * failedAttempts;
        double nightFactor = isNight() ? settings.getNightFactor() : 0.0;
        double finalThreshold = base + attemptPenalty + nightFactor;
        return Math.min(finalThreshold, settings.getMaxThreshold());
    }

    private boolean isNight() {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(22, 0)) || now.isBefore(LocalTime.of(6, 0));
    }
}