package com.vkr.auth.service;

import com.vkr.auth.dto.FaceAuthenticationResult;
import com.vkr.auth.model.User;
import java.util.Optional;

public interface FaceRecognitionService {
    void registerFace(User user, byte[] imageData);
    Optional<FaceAuthenticationResult> authenticate(byte[] imageData);
}