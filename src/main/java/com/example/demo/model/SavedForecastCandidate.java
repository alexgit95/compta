package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saved_forecast_candidate")
@Getter @Setter @NoArgsConstructor
public class SavedForecastCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String cadence;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saved_forecast_occurrence", joinColumns = @JoinColumn(name = "candidate_id"))
    @OrderColumn(name = "date_position")
    @Column(name = "occurrence_date", nullable = false)
    private List<LocalDate> dates = new ArrayList<>();
}