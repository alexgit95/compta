package com.example.demo.service;

import com.example.demo.model.ApiKey;
import com.example.demo.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final int KEY_BYTES = 32;
    private static final int PREFIX_LENGTH = 8;

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public List<ApiKey> findAll() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Creates a new API key. Returns the raw key (shown once).
     */
    @Transactional
    public String createKey(String name, int validityDays) {
        byte[] bytes = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(bytes);
        String rawKey = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String prefix = rawKey.substring(0, PREFIX_LENGTH);

        ApiKey apiKey = new ApiKey();
        apiKey.setName(name);
        apiKey.setKeyHash(passwordEncoder.encode(rawKey));
        apiKey.setKeyPrefix(prefix);
        apiKey.setExpiresAt(LocalDateTime.now().plusDays(validityDays));
        apiKeyRepository.save(apiKey);
        return rawKey;
    }

    @Transactional
    public void revoke(Long id) {
        apiKeyRepository.findById(id).ifPresent(k -> {
            k.setRevoked(true);
            apiKeyRepository.save(k);
        });
    }

    /**
     * Validates a raw API key. Updates lastUsedAt on success.
     */
    @Transactional
    public Optional<ApiKey> validate(String rawKey) {
        if (rawKey == null || rawKey.length() < PREFIX_LENGTH) {
            return Optional.empty();
        }
        String prefix = rawKey.substring(0, PREFIX_LENGTH);
        return apiKeyRepository.findByKeyPrefix(prefix)
                .filter(k -> !k.isRevoked())
                .filter(k -> k.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(k -> passwordEncoder.matches(rawKey, k.getKeyHash()))
                .map(k -> {
                    k.setLastUsedAt(LocalDateTime.now());
                    return apiKeyRepository.save(k);
                });
    }
}
