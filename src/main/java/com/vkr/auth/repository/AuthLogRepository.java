package com.vkr.auth.repository;

import com.vkr.auth.model.AuthLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface AuthLogRepository extends JpaRepository<AuthLog, String> {
    Page<AuthLog> findByMethod(String method, Pageable pageable);
    Page<AuthLog> findBySuccess(boolean success, Pageable pageable);
    Page<AuthLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    long countBySuccess(boolean success);

    @Modifying
    @Query("DELETE FROM AuthLog a WHERE a.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}