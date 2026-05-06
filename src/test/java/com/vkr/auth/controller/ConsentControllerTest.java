package com.vkr.auth.controller;

import com.vkr.auth.dto.ConsentRequest;
import com.vkr.auth.service.ConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConsentControllerTest {

    private MockMvc mockMvc;
    private final ConsentService consentService = mock(ConsentService.class);

    @BeforeEach
    void setUp() {
        ConsentController controller = new ConsentController(consentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void recordConsent_ShouldReturnSuccess_WhenValidRequest() throws Exception {
        String json = "{\"agreed\":true, \"consentVersion\":\"1.0\"}";
        mockMvc.perform(post("/api/consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .principal(() -> "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void recordConsent_ShouldReturn400_WhenNotAgreed() throws Exception {
        String json = "{\"agreed\":false, \"consentVersion\":\"1.0\"}";
        mockMvc.perform(post("/api/consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .principal(() -> "testuser"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("consent_required"));
    }
}