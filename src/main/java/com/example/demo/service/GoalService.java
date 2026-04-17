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
     * trend computed over the last {@code trendMonths} months (average monthly growth).
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

        LocalDate trendStart = now.minusMonths(trendMonths);
        BigDecimal balanceAtStart = savingsService.projectBalance(goal.getSavingsAccount(), trendStart);

        long totalMonths = trendMonths;
        if (totalMonths <= 0) return Optional.empty();

        BigDecimal totalGrowth = currentBalance.subtract(balanceAtStart);
        if (totalGrowth.compareTo(BigDecimal.ZERO) <= 0) return Optional.empty();

        BigDecimal monthlyGrowth = totalGrowth.divide(BigDecimal.valueOf(totalMonths), 4, RoundingMode.HALF_UP);
        if (monthlyGrowth.compareTo(BigDecimal.ZERO) <= 0) return Optional.empty();

        BigDecimal remaining = target.subtract(currentBalance);
        long monthsToReach = remaining.divide(monthlyGrowth, 0, RoundingMode.CEILING).longValue();
        if (monthsToReach < 0) return Optional.empty();

        return Optional.of(now.plusMonths(monthsToReach));
    }

    /**
     * Estimates the date using linear regression on real SavingsEntry records
     * (matches the graphical trend line shown in the chart).
     */
    public Optional<LocalDate> estimatedReachDateByTrend(Goal goal, int trendMonths) {
        if (goal.getType() != GoalType.TARGET_BALANCE) return Optional.empty();

        LocalDate now = LocalDate.now();
        BigDecimal currentBalance = savingsService.projectBalance(goal.getSavingsAccount(), now);
        BigDecimal target = goal.getTargetAmount();

        if (currentBalance.compareTo(target) >= 0) return Optional.empty();

        List<SavingsEntry> entries = savingsService.findEntriesForAccount(goal.getSavingsAccount());
        if (entries.size() < 2) return Optional.empty();

        LocalDate trendStart = now.minusMonths(trendMonths);
        List<SavingsEntry> trendEntries = entries.stream()
                .filter(e -> !e.getEntryDate().withDayOfMonth(1).isBefore(trendStart.withDayOfMonth(1)))
                .toList();

        if (trendEntries.size() < 2) return Optional.empty();

        LocalDate origin = trendEntries.get(0).getEntryDate().withDayOfMonth(1);
        double[] x = new double[trendEntries.size()];
        double[] y = new double[trendEntries.size()];
        for (int i = 0; i < trendEntries.size(); i++) {
            x[i] = java.time.temporal.ChronoUnit.MONTHS.between(origin, trendEntries.get(i).getEntryDate().withDayOfMonth(1));
            y[i] = trendEntries.get(i).getBalance().doubleValue();
        }

        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i]; sumY += y[i];
            sumXY += x[i] * y[i]; sumX2 += x[i] * x[i];
        }
        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 1e-10) return Optional.empty();
        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;

        if (slope <= 0) return Optional.empty();

        double monthsFromOrigin = (target.doubleValue() - intercept) / slope;
        if (monthsFromOrigin < 0) return Optional.empty();

        LocalDate estimated = origin.plusMonths((long) Math.ceil(monthsFromOrigin));
        if (estimated.isBefore(now)) return Optional.of(now.plusMonths(1));
        return Optional.of(estimated);
    }

    /**
     * Estimates the date using the configured monthly deposit (matches the projection
     * curve shown in the chart: linear extrapolation from current balance at monthlyDeposit/month).
     */
    public Optional<LocalDate> estimatedReachDateByProjection(Goal goal) {
        if (goal.getType() != GoalType.TARGET_BALANCE) return Optional.empty();

        LocalDate now = LocalDate.now();
        BigDecimal currentBalance = savingsService.projectBalance(goal.getSavingsAccount(), now);
        BigDecimal target = goal.getTargetAmount();

        if (currentBalance.compareTo(target) >= 0) return Optional.empty();

        BigDecimal monthlyDeposit = goal.getSavingsAccount().getMonthlyDeposit();
        if (monthlyDeposit == null || monthlyDeposit.compareTo(BigDecimal.ZERO) <= 0) return Optional.empty();

        BigDecimal remaining = target.subtract(currentBalance);
        long months = remaining.divide(monthlyDeposit, 0, RoundingMode.CEILING).longValue();
        return Optional.of(now.plusMonths(months));
    }

    /**
     * Returns both trend (regression) and projection (monthly deposit) reach dates.
     */
    public Map<String, LocalDate> estimatedReachDates(Goal goal, int trendMonths) {
        Map<String, LocalDate> map = new LinkedHashMap<>();
        map.put("trend", estimatedReachDateByTrend(goal, trendMonths).orElse(null));
        map.put("projection", estimatedReachDateByProjection(goal).orElse(null));
        return map;
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
