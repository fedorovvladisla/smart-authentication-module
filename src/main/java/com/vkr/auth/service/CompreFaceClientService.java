package com.vkr.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vkr.auth.dto.FaceAuthenticationResult;
import com.vkr.auth.model.User;
import com.vkr.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompreFaceClientService implements FaceRecognitionService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${compreface.base-url}")
    private String baseUrl;

    @Value("${compreface.api-key}")
    private String apiKey;

    @Override
    public void registerFace(User user, byte[] imageData) {
        String url = baseUrl + "/api/v1/recognition/faces?subject=" + user.getId();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("x-api-key", apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return "face.jpg";
            }
        };
        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForObject(url, requestEntity, String.class);
            log.info("User {} registered in CompreFace", user.getUsername());
        } catch (Exception e) {
            log.error("CompreFace registration failed", e);
            throw new RuntimeException("Face registration error", e);
        }
    }

    @Override
    public Optional<FaceAuthenticationResult> authenticate(byte[] imageData) {
        String url = baseUrl + "/api/v1/recognition/recognize";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("x-api-key", apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return "face.jpg";
            }
        };
        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<RecognitionResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, RecognitionResponse.class);
            RecognitionResponse respBody = response.getBody();
            if (respBody != null && respBody.getResult() != null && !respBody.getResult().isEmpty()) {
                List<RecognitionResponse.Subject> subjects = respBody.getResult().get(0).getSubjects();
                if (subjects != null && !subjects.isEmpty()) {
                    RecognitionResponse.Subject firstSubject = subjects.get(0);
                    String subjectId = firstSubject.getSubject();
                    double similarity = firstSubject.getSimilarity();
                    Optional<User> userOpt = userRepository.findById(subjectId);
                    if (userOpt.isPresent()) {
                        return Optional.of(new FaceAuthenticationResult(userOpt.get(), similarity));
                    }
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
            private List<Subject> subjects;
            public List<Subject> getSubjects() { return subjects; }
            public void setSubjects(List<Subject> subjects) { this.subjects = subjects; }
        }

        static class Subject {
            private String subject;
            private double similarity;
            public String getSubject() { return subject; }
            public void setSubject(String subject) { this.subject = subject; }
            public double getSimilarity() { return similarity; }
            public void setSimilarity(double similarity) { this.similarity = similarity; }
        }
    }
}