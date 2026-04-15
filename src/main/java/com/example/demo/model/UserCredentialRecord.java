package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity used solely to let Hibernate create/update the {@code user_credentials} table
 * that {@link org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository}
 * expects at runtime. Column names mirror exactly the Spring Security WebAuthn schema.
 */
@Entity
@Table(name = "user_credentials")
@Getter @Setter @NoArgsConstructor
public class UserCredentialRecord {

    @Id
    @Column(name = "credential_id", length = 1000)
    private String credentialId;

    @Column(name = "user_entity_user_id", nullable = false, length = 1000)
    private String userEntityUserId;

    @Lob
    @Column(name = "public_key", nullable = false, columnDefinition = "bytea")
    private byte[] publicKey;

    @Column(name = "signature_count")
    private Long signatureCount;

    @Column(name = "uv_initialized")
    private Boolean uvInitialized;

    @Column(name = "backup_eligible", nullable = false)
    private Boolean backupEligible = false;

    @Column(name = "authenticator_transports", length = 1000)
    private String authenticatorTransports;

    @Column(name = "public_key_credential_type", length = 100)
    private String publicKeyCredentialType;

    @Column(name = "backup_state", nullable = false)
    private Boolean backupState = false;

    @Lob
    @Column(name = "attestation_object", columnDefinition = "bytea")
    private byte[] attestationObject;

    @Lob
    @Column(name = "attestation_client_data_json", columnDefinition = "bytea")
    private byte[] attestationClientDataJson;

    private Instant created;

    @Column(name = "last_used")
    private Instant lastUsed;

    @Column(nullable = false, length = 1000)
    private String label;
}
