package com.vkr.auth.service;

import com.vkr.auth.model.AppSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicThresholdServiceTest {

    @Mock
    private AppSettingsService settingsService;
    @Mock
    private Clock clock;   // замокаем часы

    @InjectMocks
    private DynamicThresholdService thresholdService;

    private AppSettings settings;

    @BeforeEach
    void setUp() {
        settings = new AppSettings();
        settings.setBaseThreshold(0.7);
        settings.setAttemptPenalty(0.05);
        settings.setNightFactor(0.1);
        settings.setMaxThreshold(0.95);
        when(settingsService.getSettings()).thenReturn(settings);

        // По умолчанию делаем так, что день (12:00)
        when(clock.instant()).thenReturn(
                LocalDateTime.of(2026, 1, 1, 12, 0).toInstant(ZoneOffset.UTC)
        );
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void shouldReturnBaseThresholdWhenNoFailedAttemptsAndDaytime() {
        double result = thresholdService.computeThreshold(0);
        assertThat(result).isEqualTo(0.7);
    }

    @Test
    void shouldAddPenaltyForFailedAttempts() {
        double result = thresholdService.computeThreshold(3);
        assertThat(result).isEqualTo(0.85);   // 0.7 + 3*0.05 = 0.85
    }

    @Test
    void shouldAddNightFactorIfNight() {
        // Перематываем время на 3 часа ночи
        when(clock.instant()).thenReturn(
                LocalDateTime.of(2026, 1, 1, 3, 0).toInstant(ZoneOffset.UTC)
        );
        double result = thresholdService.computeThreshold(0);
        assertThat(result).isCloseTo(0.8, within(0.0001));
    }

    @Test
    void shouldNotExceedMaxThreshold() {
        double result = thresholdService.computeThreshold(10); // 0.7 + 0.5 = 1.2, capped at 0.95
        assertThat(result).isEqualTo(0.95);
    }

    @Test
    void shouldNotAddNightFactorIfJustBeforeNightStart() {
        when(clock.instant()).thenReturn(
                LocalDateTime.of(2026, 1, 1, 21, 59).toInstant(ZoneOffset.UTC)
        );
        double result = thresholdService.computeThreshold(0);
        assertThat(result).isEqualTo(0.7);
    }
}