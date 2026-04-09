package com.vkr.auth.service;

import com.vkr.auth.dto.FaceAuthenticationResult;
import com.vkr.auth.model.User;
import com.vkr.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompreFaceClientService implements FaceRecognitionService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${compreface.recognition-url}")
    private String recognitionUrl;

    @Override
    public void registerFace(User user, byte[] imageData) {
        String url = recognitionUrl + "/subjects?subject=" + user.getId();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<byte[]> request = new HttpEntity<>(imageData, headers);

        try {
            restTemplate.postForObject(url, request, String.class);
            log.info("User {} registered in CompreFace", user.getUsername());
        } catch (Exception e) {
            log.error("CompreFace registration failed", e);
            throw new RuntimeException("Face registration error", e);
        }
    }

    @Override
    public Optional<FaceAuthenticationResult> authenticate(byte[] imageData) {
        String url = recognitionUrl + "/recognize";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<byte[]> request = new HttpEntity<>(imageData, headers);

        try {
            ResponseEntity<RecognitionResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, RecognitionResponse.class);
            RecognitionResponse body = response.getBody();
            if (body != null && body.getResult() != null && !body.getResult().isEmpty()) {
                RecognitionResponse.Result first = body.getResult().get(0);
                Optional<User> userOpt = userRepository.findById(first.getSubject());
                if (userOpt.isPresent()) {
                    return Optional.of(new FaceAuthenticationResult(userOpt.get(), first.getSimilarity()));
                }
            }
        } catch (Exception e) {
            log.error("CompreFace recognition failed", e);
        }
        return Optional.empty();
    }

    static class RecognitionResponse {
        private List<Result> result;
        public List<Result> getResult() { return result; }
        public void setResult(List<Result> result) { this.result = result; }

        static class Result {
            private String subject;
            private double similarity;
            public String getSubject() { return subject; }
            public void setSubject(String subject) { this.subject = subject; }
            public double getSimilarity() { return similarity; }
            public void setSimilarity(double similarity) { this.similarity = similarity; }
        }
    }
}