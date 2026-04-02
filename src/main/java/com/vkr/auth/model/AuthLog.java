package com.vkr.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class AuthLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    @Column(nullable = false)
    private String username; // денормализация

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String method; // PASSKEY, FACE, PASSWORD

    @Column(nullable = false)
    private String ipAddress;

    private String geoLocation;

    @Column(nullable = false)
    private boolean success;

    private String failureReason;

    private double confidence;
}