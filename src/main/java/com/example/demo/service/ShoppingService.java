package com.example.demo.service;

import com.example.demo.model.ShoppingSettings;
import com.example.demo.repository.ShoppingSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Service for managing shopping (groceries) settings and calculations.
 * Handles frequency-based shopping budget projections.
 */
@Service
@RequiredArgsConstructor
public class ShoppingService {

    private final ShoppingSettingsRepository repository;

    public List<ShoppingSettings> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void save(ShoppingSettings settings) {
        repository.save(settings);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Calculate the number of remaining shopping trips for the current month.
     * 
     * @param settings the shopping configuration
     * @param today the current date
     * @return number of remaining shopping trips (0 or more)
     */
    public int getRemainingShoppingTripsThisMonth(ShoppingSettings settings, LocalDate today) {
        if (settings == null) {
            return 0;
        }

        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        LocalDate nextShoppingDate = settings.getLastShoppingDate().plusDays(settings.getFrequencyDays());

        int trips = 0;
        LocalDate current = nextShoppingDate;

        while (!current.isAfter(monthEnd)) {
            trips++;
            current = current.plusDays(settings.getFrequencyDays());
        }

        return trips;
    }

    /**
     * Calculate the total remaining shopping budget for the current month.
     * 
     * @param settings the shopping configuration
     * @param today the current date
     * @return total remaining budget (amount × remaining trips)
     */
    public BigDecimal getRemainingShoppingBudgetThisMonth(ShoppingSettings settings, LocalDate today) {
        if (settings == null || settings.getAmount() == null) {
            return BigDecimal.ZERO;
        }

        int trips = getRemainingShoppingTripsThisMonth(settings, today);
        return settings.getAmount().multiply(BigDecimal.valueOf(trips));
    }

    /**
     * Get the next expected shopping date.
     * 
     * @param settings the shopping configuration
     * @return next shopping date
     */
    public LocalDate getNextShoppingDate(ShoppingSettings settings) {
        if (settings == null) {
            return null;
        }
        return settings.getLastShoppingDate().plusDays(settings.getFrequencyDays());
    }
}
