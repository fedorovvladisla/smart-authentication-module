package com.vkr.auth.repository;

import com.vkr.auth.model.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConsentRepository extends JpaRepository<UserConsent, String> {
    Optional<UserConsent> findByUserId(String userId);
}