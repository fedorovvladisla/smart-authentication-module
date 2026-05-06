package com.vkr.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Entity
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WebAuthnCredential {

    @Id
    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String credentialId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    // Разрешаем null, потому что хранить будем только COSE-ключ
    @Column(nullable = true, length = 4096)
    private String publicKeyPem;

    @Column(nullable = false)
    private long counter;

    @Column(columnDefinition = "BYTEA")   // или @Lob
    private byte[] publicKeyCose;
}