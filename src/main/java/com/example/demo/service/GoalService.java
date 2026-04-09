package com.example.demo.service;

import com.example.demo.model.Goal;
import com.example.demo.model.GoalType;
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
     * trend computed over the last {@code trendYears} years (average monthly growth).
     * Returns empty if the goal is already reached or if the trend is not positive.
     */
    public Optional<LocalDate> estimatedReachDate(Goal goal, int trendYears) {
        if (goal.getType() != GoalType.TARGET_BALANCE) return Optional.empty();

        LocalDate now = LocalDate.now();
        BigDecimal currentBalance = savingsService.projectBalance(goal.getSavingsAccount(), now);
        BigDecimal target = goal.getTargetAmount();

        if (currentBalance.compareTo(target) >= 0) {
            return Optional.empty(); // already reached
        }

        LocalDate trendStart = now.minusYears(trendYears);
        BigDecimal balanceAtStart = savingsService.projectBalance(goal.getSavingsAccount(), trendStart);

        long totalMonths = (long) trendYears * 12;
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
     * Returns monthly chart data for a TARGET_BALANCE goal covering from
     * {@code trendYears} ago up to the estimated reach date (capped at +10 years).
     */
    public List<Map<String, Object>> getBalanceGoalChartData(Goal goal, int trendYears) {
        LocalDate now = LocalDate.now();
        LocalDate from = now.minusYears(trendYears).withDayOfMonth(1);
        BigDecimal currentBalance = savingsService.projectBalance(goal.getSavingsAccount(), now);
        boolean alreadyReached = currentBalance.compareTo(goal.getTargetAmount()) >= 0;

        Optional<LocalDate> reachDate = estimatedReachDate(goal, trendYears);
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
