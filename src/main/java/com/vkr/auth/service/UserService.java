package com.vkr.auth.service;

import com.vkr.auth.model.Role;
import com.vkr.auth.model.User;
import com.vkr.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public User findByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsernameAndBlockedFalse(String username) {
        return userRepository.findByUsernameAndBlockedFalse(username);
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public List<User> findByBlocked(boolean blocked) {
        return userRepository.findByBlocked(blocked);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username); // новый метод в репозитории
    }

    @Transactional
    public User save(User user) {
        log.debug("Saving user: {}", user.getUsername());
        return userRepository.save(user);
    }

    @Transactional
    public void blockUser(String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setBlocked(true);
        userRepository.save(user);
        log.info("User {} blocked", userId);
    }

    @Transactional
    public void unblockUser(String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setBlocked(false);
        userRepository.save(user);
        log.info("User {} unblocked", userId);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
}