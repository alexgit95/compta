package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "savings_account_type")
@Getter @Setter @NoArgsConstructor
public class SavingsAccountType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name (e.g. "Livret A", "PEA", "Assurance Vie") */
    @Column(nullable = false, unique = true)
    private String name;

    /** Emoji or short icon identifier */
    @Column(nullable = false)
    private String icon;

    /** Recommended allocation percentage (0-100) */
    @Column(nullable = false)
    private int recommendedPercentage;

    public SavingsAccountType(String name, String icon, int recommendedPercentage) {
        this.name = name;
        this.icon = icon;
        this.recommendedPercentage = recommendedPercentage;
    }
}
