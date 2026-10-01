package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saved_forecast_analysis")
@Getter @Setter @NoArgsConstructor
public class SavedForecastAnalysis {
    @Id
    private Long id;

    private int fromYear;
    private int fromMonth;
    private int toYear;
    private int toMonth;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal tolerancePercent;

    @Column(nullable = false)
    private Instant analyzedAt;

    @Column(nullable = false)
    private boolean valid;

    @Column(nullable = false, length = 64)
    private String sourceFingerprint;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "analysis_id")
    @OrderColumn(name = "candidate_position")
    private List<SavedForecastCandidate> candidates = new ArrayList<>();

    public YearMonth from() {
        return YearMonth.of(fromYear, fromMonth);
    }

    public YearMonth to() {
        return YearMonth.of(toYear, toMonth);
    }
}