package com.vkr.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginInitResponse {
    private String method; // "PASSKEY", "FACE", "PASSWORD"
}