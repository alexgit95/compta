package com.example.demo.repository;

import com.example.demo.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    List<ApiKey> findAllByOrderByCreatedAtDesc();
    Optional<ApiKey> findByKeyPrefix(String prefix);
    List<ApiKey> findByRevokedFalseAndExpiresAtAfter(LocalDateTime now);
}
