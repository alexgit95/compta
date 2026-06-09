package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Generic key-value store for application-wide configurable settings.
 * Numeric settings store their value in {@code numericValue}.
 */
@Entity
@Table(name = "app_setting")
@Getter @Setter @NoArgsConstructor
public class AppSetting {

    @Id
    @Column(nullable = false, unique = true, length = 100)
    private String key;

    /** Human-readable label shown in the admin UI. */
    @Column(nullable = false, length = 200)
    private String label;

    /** Numeric value (used for salary, courses budget, …). */
    @Column(precision = 15, scale = 2)
    private BigDecimal numericValue;

    public AppSetting(String key, String label, BigDecimal numericValue) {
        this.key = key;
        this.label = label;
        this.numericValue = numericValue;
    }
}
