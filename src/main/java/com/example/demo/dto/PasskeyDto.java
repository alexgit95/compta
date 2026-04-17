package com.example.demo.dto;

/**
 * Lightweight view-model for a single registered passkey (CredentialRecord),
 * carrying only the data the Thymeleaf template needs.
 */
public record PasskeyDto(
        String label,
        String credentialIdBase64Url,
        String created,
        String lastUsed
) {}
