package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Configuration for shopping (groceries) management.
 * Tracks frequency and amount of shopping to project remaining budget for the month.
 */
@Entity
@Table(name = "shopping_settings")
@Getter @Setter @NoArgsConstructor
public class ShoppingSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Amount spent per shopping trip (e.g., 80.00 €) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Frequency in days (e.g., 7 for weekly) */
    @Column(nullable = false)
    private Integer frequencyDays;

    /** Last date when shopping was done */
    @Column(nullable = false)
    private LocalDate lastShoppingDate;

    public ShoppingSettings(BigDecimal amount, Integer frequencyDays, LocalDate lastShoppingDate) {
        this.amount = amount;
        this.frequencyDays = frequencyDays;
        this.lastShoppingDate = lastShoppingDate;
    }
}
