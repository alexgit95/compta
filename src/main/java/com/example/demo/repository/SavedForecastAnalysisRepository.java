package com.example.demo.repository;

import com.example.demo.model.SavedForecastAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedForecastAnalysisRepository extends JpaRepository<SavedForecastAnalysis, Long> {
}