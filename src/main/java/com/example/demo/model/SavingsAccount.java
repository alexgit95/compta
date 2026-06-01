package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "savings_account")
@Getter @Setter @NoArgsConstructor
public class SavingsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    /** Amount deposited each month */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyDeposit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_type_id")
    private SavingsAccountType accountType;

    /**
     * true  = épargne long terme (prise en compte dans le patrimoine)
     * false = fond de roulement (dépenses courantes, travaux, voyages…)
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean longTermSavings = false;
}
