package com.vkr.auth.repository;

import com.vkr.auth.model.Role;
import com.vkr.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndBlockedFalse(String username);
    List<User> findByRole(Role role);
    List<User> findByBlocked(boolean blocked);
}