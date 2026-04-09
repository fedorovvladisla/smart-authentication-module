package com.vkr.auth.service;

import com.vkr.auth.model.AppSettings;
import com.vkr.auth.repository.AppSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class AppSettingsService {

    private final AppSettingsRepository settingsRepository;

    @PostConstruct
    public void init() {
        if (settingsRepository.findById(1L).isEmpty()) {
            AppSettings defaultSettings = new AppSettings();
            defaultSettings.setId(1L);
            defaultSettings.setBaseThreshold(0.7);
            defaultSettings.setAttemptPenalty(0.05);
            defaultSettings.setNightFactor(0.1);
            defaultSettings.setMaxThreshold(0.95);
            settingsRepository.save(defaultSettings);
        }
    }

    @Transactional(readOnly = true)
    public AppSettings getSettings() {
        return settingsRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Settings not initialized"));
    }

    @Transactional
    public void updateSettings(AppSettings newSettings) {
        newSettings.setId(1L);
        settingsRepository.save(newSettings);
    }

    public double getBaseThreshold() { return getSettings().getBaseThreshold(); }
    public double getAttemptPenalty() { return getSettings().getAttemptPenalty(); }
    public double getNightFactor() { return getSettings().getNightFactor(); }
    public double getMaxThreshold() { return getSettings().getMaxThreshold(); }
}