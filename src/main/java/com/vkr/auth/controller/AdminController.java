package com.vkr.auth.controller;

import com.vkr.auth.dto.UserListItemDto;
import com.vkr.auth.model.AppSettings;
import com.vkr.auth.model.AuthLog;
import com.vkr.auth.model.User;
import com.vkr.auth.repository.AppSettingsRepository;
import com.vkr.auth.service.AuthLogService;
import com.vkr.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthLogService authLogService;
    private final UserService userService;
    private final AppSettingsRepository settingsRepository;

    // Логи с пагинацией
    @GetMapping("/logs")
    public String logs(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "50") int size,
                       Model model) {
        Page<AuthLog> logsPage = authLogService.findAll(PageRequest.of(page, size, Sort.by("timestamp").descending()));
        model.addAttribute("logs", logsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logsPage.getTotalPages());
        return "admin/logs";
    }

    // Список пользователей
    @GetMapping("/users")
    public String users(Model model) {
        List<User> users = userService.findAll();
        List<UserListItemDto> userDtos = users.stream()
                .map(u -> new UserListItemDto(u.getId(), u.getUsername(), u.getRole().name(), u.isBlocked()))
                .collect(Collectors.toList());
        model.addAttribute("users", userDtos);
        return "admin/users";
    }

    @PostMapping("/users/{id}/block")
    public String blockUser(@PathVariable String id) {
        userService.blockUser(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/unblock")
    public String unblockUser(@PathVariable String id) {
        userService.unblockUser(id);
        return "redirect:/admin/users";
    }

    // Настройки системы
    @GetMapping("/settings")
    public String settings(Model model) {
        AppSettings settings = settingsRepository.findById(1L)
                .orElseGet(() -> {
                    AppSettings s = new AppSettings();
                    s.setId(1L);
                    s.setBaseThreshold(0.7);
                    s.setAttemptPenalty(0.05);
                    s.setNightFactor(0.1);
                    s.setMaxThreshold(0.95);
                    return s;
                });
        model.addAttribute("settings", settings);
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@ModelAttribute AppSettings settings) {
        settings.setId(1L);
        settingsRepository.save(settings);
        return "redirect:/admin/settings";
    }
}