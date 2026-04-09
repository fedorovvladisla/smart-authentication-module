package com.vkr.auth.dto;

import com.vkr.auth.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FaceAuthenticationResult {
    private final User user;
    private final double confidence;
}