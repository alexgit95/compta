package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "statement_row", uniqueConstraints = @UniqueConstraint(columnNames = {"statement_year", "statement_month", "line_number"}))
@Getter @Setter @NoArgsConstructor
public class StatementRow {

    public enum Kind { OPERATION, BALANCE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_year", nullable = false)
    private int year;

    @Column(name = "statement_month", nullable = false)
    private int month;

    @Column(name = "line_number", nullable = false)
    private long lineNumber;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    private String transactionType;
    private String reference;
    private String debitLabel;
    private String creditLabel;
    private String extra;
    private String categoryLabel;
}