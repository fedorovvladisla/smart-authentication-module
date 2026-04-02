package com.vkr.auth.repository;

import com.vkr.auth.model.FacePrint;
import com.vkr.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacePrintRepository extends JpaRepository<FacePrint, String> {
    Optional<FacePrint> findByUser(User user);
}
