package com.vkr.auth.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class AppSettings {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private double baseThreshold = 0.7;

    @Column(nullable = false)
    private double attemptPenalty = 0.05;

    @Column(nullable = false)
    private double nightFactor = 0.1;

    @Column(nullable = false)
    private double maxThreshold = 0.95;
}