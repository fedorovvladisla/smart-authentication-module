package com.vkr.auth.controller;

import com.vkr.auth.model.AuthLog;
import com.vkr.auth.model.User;
import com.vkr.auth.model.Role;
import com.vkr.auth.model.AppSettings;
import com.vkr.auth.repository.AppSettingsRepository;
import com.vkr.auth.service.AuthLogService;
import com.vkr.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminControllerTest {

    private MockMvc mockMvc;
    private final AuthLogService authLogService = mock(AuthLogService.class);
    private final UserService userService = mock(UserService.class);
    private final AppSettingsRepository appSettingsRepository = mock(AppSettingsRepository.class);

    @BeforeEach
    void setUp() {
        AdminController controller = new AdminController(authLogService, userService, appSettingsRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // 1. Просмотр логов (успех)
    @Test
    void logs_ShouldReturnPageWithLogs() throws Exception {
        AuthLog log = new AuthLog();
        log.setId("1");
        log.setUsername("testuser");
        log.setMethod("PASSKEY");
        log.setSuccess(true);
        log.setTimestamp(LocalDateTime.now());
        log.setIpAddress("127.0.0.1");
        log.setGeoLocation("Unknown");
        log.setFailureReason(null);
        log.setConfidence(0.0);

        Page<AuthLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 50), 1);
        when(authLogService.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/admin/logs"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("logs", "currentPage", "totalPages"))
                .andExpect(view().name("admin/logs"));
    }

    // 2. Логи с параметрами пагинации
    @Test
    void logs_ShouldUsePaginationParameters() throws Exception {
        when(authLogService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/admin/logs")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 1));
    }

    // 3. Список пользователей (успех)
    @Test
    void users_ShouldReturnListOfUsers() throws Exception {
        User user = new User();
        user.setId("1");
        user.setUsername("testuser");
        user.setRole(Role.USER);
        user.setBlocked(false);

        when(userService.findAll()).thenReturn(List.of(user));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("users"))
                .andExpect(view().name("admin/users"));
    }

    // 4. Блокировка пользователя (успех)
    @Test
    void blockUser_ShouldRedirectToUsers() throws Exception {
        mockMvc.perform(post("/admin/users/123/block"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService).blockUser("123");
    }

    // 5. Разблокировка пользователя (успех)
    @Test
    void unblockUser_ShouldRedirectToUsers() throws Exception {
        mockMvc.perform(post("/admin/users/456/unblock"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService).unblockUser("456");
    }

    // 6. Настройки (существующие)
    @Test
    void settings_ShouldReturnSettingsPage() throws Exception {
        AppSettings settings = new AppSettings();
        settings.setBaseThreshold(0.7);
        when(appSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("settings"))
                .andExpect(view().name("admin/settings"));
    }

    // 7. Настройки не инициализированы – должны создаться дефолтные
    @Test
    void settings_ShouldCreateDefaultWhenNotExists() throws Exception {
        when(appSettingsRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("settings"))
                .andExpect(view().name("admin/settings"));
    }

    // 8. Обновление настроек (успех)
    @Test
    void updateSettings_ShouldSaveAndRedirect() throws Exception {
        mockMvc.perform(post("/admin/settings")
                        .param("baseThreshold", "0.8")
                        .param("attemptPenalty", "0.1")
                        .param("nightFactor", "0.2")
                        .param("maxThreshold", "0.9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"));

        verify(appSettingsRepository).save(any(AppSettings.class));
    }
}