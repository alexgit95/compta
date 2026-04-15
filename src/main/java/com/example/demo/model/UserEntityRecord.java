package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity used solely to let Hibernate create/update the {@code user_entities} table
 * that {@link org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository}
 * expects at runtime.
 */
@Entity
@Table(name = "user_entities")
@Getter @Setter @NoArgsConstructor
public class UserEntityRecord {

    @Id
    @Column(length = 1000)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", length = 200)
    private String displayName;
}
