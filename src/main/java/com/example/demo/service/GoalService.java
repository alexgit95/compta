package com.example.demo.service;

import com.example.demo.model.Goal;
import com.example.demo.model.GoalType;
import com.example.demo.model.SavingsEntry;
import com.example.demo.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final SavingsService savingsService;

    public List<Goal> findAllGoals() {
        return goalRepository.findAll();
    }

    @Transactional
    public Goal saveGoal(Goal goal) {
        return goalRepository.save(goal);
    }

    @Transactional
    public void deleteGoal(Long id) {
        goalRepository.deleteById(id);
    }

    /**
     * Estimates the date when a TARGET_BALANCE goal will be reached using a linear
     * regression trend computed over the last {@code trendMonths} months.
     * This matches the graphical trend line displayed to the user.
     * Returns empty if the goal is already reached or if the trend is not positive.
     */
    public Optional<LocalDate> estimatedReachDate(Goal goal, int trendMonths) {
        if (goal.getType() != GoalType.TARGET_BALANCE) return Optional.empty();

        LocalDate now = LocalDate.now();
        BigDecimal currentBalance = savingsService.projectBalance(goal.getSavingsAccount(), now);
        BigDecimal target = goal.getTargetAmount();

        if (currentBalance.compareTo(target) >= 0) {
            return Optional.empty(); // already reached
        }

        // Get all entries for this account to perform linear regression
        List<SavingsEntry> entries = savingsService.findEntriesForAccount(goal.getSavingsAccount());
        if (entries.size() < 2) return Optional.empty();

        // Get real entry dates within the trend period
        LocalDate trendStart = now.minusMonths(trendMonths);
        List<SavingsEntry> trendEntries = entries.stream()
                .filter(e -> !e.getEntryDate().withDayOfMonth(1).isBefore(trendStart.withDayOfMonth(1)))
                .toList();

        if (trendEntries.size() < 2) return Optional.empty();

        // Perform linear regression on months from first trend entry
        LocalDate origin = trendEntries.get(0).getEntryDate().withDayOfMonth(1);
        double[] xValues = new double[trendEntries.size()];
        double[] yValues = new double[trendEntries.size()];

        for (int i = 0; i < trendEntries.size(); i++) {
            xValues[i] = monthsDiff(origin, trendEntries.get(i).getEntryDate());
            yValues[i] = trendEntries.get(i).getBalance().doubleValue();
        }

        LinearRegressionResult reg = performLinearRegression(xValues, yValues);
        if (reg == null || reg.slope <= 0) {
            return Optional.empty(); // no positive trend
        }

        // Calculate months from origin needed to reach target using: balance = intercept + slope * months
        // target = intercept + slope * monthsFromOrigin
        // monthsFromOrigin = (target - intercept) / slope
        double monthsFromOrigin = (target.doubleValue() - reg.intercept) / reg.slope;
        if (monthsFromOrigin < 0) return Optional.empty();

        // Convert back to absolute date by adding to origin
        long monthsToAdd = Math.round(monthsFromOrigin);
        LocalDate estimatedDate = origin.plusMonths(monthsToAdd);

        // If the estimated date is in the past, adjust based on current trend
        // but at minimum return a reasonable future date
        if (estimatedDate.isBefore(now)) {
            // This shouldn't happen if trend is positive, but handle it gracefully
            return Optional.of(now.plusMonths(1));
        }

        return Optional.of(estimatedDate);
    }

    /**
     * Helper: Calculate months between two dates (same logic as JavaScript monthDiff)
     */
    private long monthsDiff(LocalDate from, LocalDate to) {
        return java.time.temporal.ChronoUnit.MONTHS.between(
                from.withDayOfMonth(1),
                to.withDayOfMonth(1)
        );
    }

    /**
     * Linear regression helper
     */
    private LinearRegressionResult performLinearRegression(double[] xValues, double[] yValues) {
        if (xValues.length != yValues.length || xValues.length < 2) return null;

        int n = xValues.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += xValues[i];
            sumY += yValues[i];
            sumXY += xValues[i] * yValues[i];
            sumX2 += xValues[i] * xValues[i];
        }

        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 1e-10) return null;

        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;

        return new LinearRegressionResult(slope, intercept);
    }

    /**
     * Simple data class for linear regression result
     */
    private static class LinearRegressionResult {
        final double slope;
        final double intercept;

        LinearRegressionResult(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
        }
    }

    /**
     * Estimates the date when a TARGET_BALANCE goal will be reached using simple
     * projection (average monthly growth rate) over the last {@code trendMonths} months.
     * This is the legacy calculation method.
     * Returns empty if the goal is already reached or if the trend is not positive.
     */
    public Optional<LocalDate> estimatedReachDateByProjection(Goal goal, int trendMonths) {
        if (goal.getType() != GoalType.TARGET_BALANCE) return Optional.empty();

        LocalDate now = LocalDate.now();
        BigDecimal currentBalance = savingsService.projectBalance(goal.getSavingsAccount(), now);
        BigDecimal target = goal.getTargetAmount();

        if (currentBalance.compareTo(target) >= 0) {
            return Optional.empty(); // already reached
        }

        // Get all entries for this account
        List<SavingsEntry> entries = savingsService.findEntriesForAccount(goal.getSavingsAccount());
        if (entries.size() < 2) return Optional.empty();

        // Get entries within the trend period
        LocalDate trendStart = now.minusMonths(trendMonths);
        List<SavingsEntry> trendEntries = entries.stream()
                .filter(e -> !e.getEntryDate().withDayOfMonth(1).isBefore(trendStart.withDayOfMonth(1)))
                .toList();

        if (trendEntries.size() < 2) return Optional.empty();

        // Simple average: (last - first) / number of months
        BigDecimal firstBalance = trendEntries.get(0).getBalance();
        BigDecimal lastBalance = trendEntries.get(trendEntries.size() - 1).getBalance();
        BigDecimal growth = lastBalance.subtract(firstBalance);

        if (growth.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty(); // no positive trend
        }

        // Average growth per month
        BigDecimal avgMonthlyGrowth = growth.divide(
                BigDecimal.valueOf(trendEntries.size() - 1),
                2,
                RoundingMode.HALF_UP
        );

        // Months needed to reach target
        BigDecimal remaining = target.subtract(currentBalance);
        BigDecimal monthsNeeded = remaining.divide(avgMonthlyGrowth, 1, RoundingMode.HALF_UP);

        LocalDate estimatedDate = now.plusMonths(monthsNeeded.longValue());
        return Optional.of(estimatedDate);
    }

    /**
     * Simple wrapper class for estimated reach dates (both trend and projection)
     */
    public static class EstimatedReachDates {
        public final LocalDate trend;      // linear regression method
        public final LocalDate projection; // simple average method

        public EstimatedReachDates(LocalDate trend, LocalDate projection) {
            this.trend = trend;
            this.projection = projection;
        }
    }

    /**
     * Returns both estimated reach dates (trend and projection) for a TARGET_BALANCE goal.
     */
    public EstimatedReachDates estimatedReachDates(Goal goal, int trendMonths) {
        Optional<LocalDate> trend = estimatedReachDate(goal, trendMonths);
        Optional<LocalDate> projection = estimatedReachDateByProjection(goal, trendMonths);
        return new EstimatedReachDates(
                trend.orElse(null),
                projection.orElse(null)
        );
    }

    /**
     * Returns monthly chart data for a TARGET_BALANCE goal covering from
     * {@code trendMonths} months ago up to the estimated reach date (capped at +10 years).
     */
    public List<Map<String, Object>> getBalanceGoalChartData(Goal goal, int trendMonths) {
        LocalDate now = LocalDate.now();
        LocalDate from = now.minusMonths(trendMonths).withDayOfMonth(1);
        BigDecimal currentBalance = savingsService.projectBalance(goal.getSavingsAccount(), now);
        boolean alreadyReached = currentBalance.compareTo(goal.getTargetAmount()) >= 0;

        Optional<LocalDate> reachDate = estimatedReachDate(goal, trendMonths);
        LocalDate to;
        if (alreadyReached) {
            to = now.plusMonths(6);
        } else {
            to = reachDate.orElse(now.plusYears(5));
            if (to.isAfter(now.plusYears(10))) to = now.plusYears(10);
        }
        return savingsService.getChartData(goal.getSavingsAccount(), from, to);
    }
}
