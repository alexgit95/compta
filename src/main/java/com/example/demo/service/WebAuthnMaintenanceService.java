package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for WebAuthn table maintenance and reset operations.
 * Allows resetting WebAuthn credentials and entities when needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebAuthnMaintenanceService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.webauthn.reset-tables:false}")
    private boolean resetTablesOnStartup;

    /**
     * Reset WebAuthn tables (drop and let Hibernate recreate them)
     * Should be called during DataInitializer
     */
    @Transactional
    public void resetWebAuthnTablesIfNeeded() {
        if (!resetTablesOnStartup) {
            return;
        }

        try {
            log.warn("Resetting WebAuthn tables (user_credentials, user_entities)");
            
            // Drop foreign key constraints first, then tables
            try {
                jdbcTemplate.execute("DROP TABLE IF EXISTS user_credentials CASCADE");
                log.info("Dropped table user_credentials");
            } catch (Exception e) {
                log.warn("Could not drop user_credentials: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("DROP TABLE IF EXISTS user_entities CASCADE");
                log.info("Dropped table user_entities");
            } catch (Exception e) {
                log.warn("Could not drop user_entities: {}", e.getMessage());
            }

            log.info("WebAuthn tables reset. They will be recreated by Hibernate.");
        } catch (Exception e) {
            log.error("Failed to reset WebAuthn tables", e);
            throw new RuntimeException("WebAuthn table reset failed", e);
        }
    }

    /**
     * Manual reset of WebAuthn credentials and entities (delete all, keep tables)
     */
    @Transactional
    public void clearWebAuthnData() {
        try {
            log.warn("Clearing all WebAuthn data");
            jdbcTemplate.execute("DELETE FROM user_credentials");
            jdbcTemplate.execute("DELETE FROM user_entities");
            log.info("WebAuthn data cleared successfully");
        } catch (Exception e) {
            log.error("Failed to clear WebAuthn data", e);
            throw new RuntimeException("WebAuthn data clear failed", e);
        }
    }
}
