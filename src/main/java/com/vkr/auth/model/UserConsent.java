package com.vkr.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class UserConsent {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private LocalDateTime consentTimestamp;

    @Column(nullable = false)
    private String consentVersion;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String userAgent;
}