package com.example.demo.repository;

import com.example.demo.model.ShoppingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingSettingsRepository extends JpaRepository<ShoppingSettings, Long> {
}
