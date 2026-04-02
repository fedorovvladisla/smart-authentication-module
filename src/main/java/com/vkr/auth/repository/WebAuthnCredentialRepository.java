package com.vkr.auth.repository;

import com.vkr.auth.model.User;
import com.vkr.auth.model.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, String> {
    List<WebAuthnCredential> findByUser(User user);
}
