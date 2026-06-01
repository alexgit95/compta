package com.example.demo.service;

import com.example.demo.model.Credit;
import com.example.demo.model.Property;
import com.example.demo.model.SavingsAccount;
import com.example.demo.repository.CreditRepository;
import com.example.demo.repository.PropertyRepository;
import com.example.demo.repository.SavingsEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatrimoineService {

    private final PropertyRepository propertyRepository;
    private final CreditRepository creditRepository;
    private final SavingsService savingsService;
    private final CreditService creditService;
    private final SavingsEntryRepository savingsEntryRepository;

    /** Accounts flagged as longTermSavings = true on the account itself. */
    private List<SavingsAccount> getLongTermAccounts() {
        return savingsService.findAllAccounts().stream()
                .filter(SavingsAccount::isLongTermSavings)
                .collect(Collectors.toList());
    }

    /** Sum of all properties' current market value. */
    public BigDecimal getTotalPropertyValue() {
        return propertyRepository.findAll().stream()
                .map(Property::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sum of long-term savings accounts' projected balance today. */
    public BigDecimal getTotalSavingsValue() {
        LocalDate today = LocalDate.now();
        BigDecimal total = BigDecimal.ZERO;
        for (SavingsAccount account : getLongTermAccounts()) {
            total = total.add(savingsService.projectBalance(account, today));
        }
        return total;
    }

    /** Total remaining amount across all credits. */
    public BigDecimal getTotalRemainingCredits() {
        return creditRepository.findAll().stream()
                .map(Credit::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Patrimoine brut = property value + long-term savings */
    public BigDecimal getGrossPatrimoine() {
        return getTotalPropertyValue().add(getTotalSavingsValue());
    }

    /** Patrimoine net = patrimoine brut - remaining credits */
    public BigDecimal getNetPatrimoine() {
        return getGrossPatrimoine().subtract(getTotalRemainingCredits());
    }

    /**
     * Generates monthly chart data for patrimoine brut and net over time.
     * @param mode       "projection" (monthly deposits) or "tendance" (linear regression)
     * @param trendMonths history window for regression (6, 12, 24, 60)
     */
    public Map<String, Object> getChartData(String mode, int trendMonths) {
        LocalDate today = LocalDate.now();
        LocalDate chartStart = today.minusMonths(trendMonths).withDayOfMonth(1);

        List<Credit> credits = creditRepository.findAllByOrderByEndDateAsc();
        LocalDate chartEnd = today.plusMonths(12);
        if (!credits.isEmpty()) {
            LocalDate maxCreditEnd = credits.stream()
                    .map(Credit::getEndDate).max(Comparator.naturalOrder()).orElse(chartEnd);
            if (maxCreditEnd.isAfter(chartEnd)) chartEnd = maxCreditEnd;
        }

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
        List<String> labels = new ArrayList<>();
        LocalDate cursor = chartStart;
        while (!cursor.isAfter(chartEnd.withDayOfMonth(1))) {
            labels.add(cursor.format(labelFmt));
            cursor = cursor.plusMonths(1);
        }
        int numMonths = labels.size();

        BigDecimal propertyValue = getTotalPropertyValue();

        double[] totalCreditRemaining = new double[numMonths];
        for (Credit credit : credits) {
            List<Double> proj = creditService.projectMonthlyRemaining(credit, chartStart, chartEnd);
            for (int i = 0; i < proj.size() && i < numMonths; i++) {
                if (proj.get(i) != null) totalCreditRemaining[i] += proj.get(i);
            }
        }

        double[] savingsProjection = "projection".equals(mode)
                ? computeSavingsDepositsProjection(chartStart, chartEnd, numMonths)
                : computeSavingsTrendProjection(chartStart, chartEnd, numMonths, trendMonths);

        List<Double> grossData = new ArrayList<>();
        List<Double> netData = new ArrayList<>();
        for (int i = 0; i < numMonths; i++) {
            double gross = propertyValue.doubleValue() + savingsProjection[i];
            double net   = gross - totalCreditRemaining[i];
            grossData.add(Math.round(gross * 100.0) / 100.0);
            netData.add(Math.round(net   * 100.0) / 100.0);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels",    labels);
        result.put("grossData", grossData);
        result.put("netData",   netData);
        return result;
    }

    /**
     * Returns projection table rows for the patrimoine page.
     * Each row: {label, gross, net}
     */
    public List<Map<String, Object>> getProjectionRows(String mode, int trendMonths) {
        LocalDate today = LocalDate.now().withDayOfMonth(1);
        int[] horizons = {0, 6, 12, 60};
        String[] labels = {"Aujourd'hui", "Dans 6 mois", "Dans 1 an", "Dans 5 ans"};

        List<Credit> credits = creditRepository.findAllByOrderByEndDateAsc();
        BigDecimal propertyValue = getTotalPropertyValue();

        // For trend regression, pre-compute slope/intercept from history
        double[] regressionParams = null;
        double todayLongTermTotal = 0;
        double totalMonthlyDeposits = 0;
        List<SavingsAccount> ltAccounts = getLongTermAccounts();

        for (SavingsAccount a : ltAccounts) {
            todayLongTermTotal += savingsService.projectBalance(a, today).doubleValue();
            totalMonthlyDeposits += a.getMonthlyDeposit().doubleValue();
        }

        if ("tendance".equals(mode)) {
            regressionParams = computeRegressionParams(today, trendMonths, ltAccounts);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int h = 0; h < horizons.length; h++) {
            LocalDate target = today.plusMonths(horizons[h]);

            // Savings at target
            double savingsAtTarget;
            if (horizons[h] == 0) {
                savingsAtTarget = todayLongTermTotal;
            } else if ("projection".equals(mode)) {
                savingsAtTarget = todayLongTermTotal + totalMonthlyDeposits * horizons[h];
            } else {
                // tendance: apply regression
                if (regressionParams != null) {
                    // regression was built on index 0 = today; x = horizons[h]
                    savingsAtTarget = Math.max(0, regressionParams[1] + regressionParams[0] * horizons[h]);
                } else {
                    savingsAtTarget = todayLongTermTotal;
                }
            }

            // Credits remaining at target
            double creditAtTarget = 0;
            for (Credit credit : credits) {
                List<Double> proj = creditService.projectMonthlyRemaining(credit, today, target);
                if (!proj.isEmpty()) {
                    Double last = null;
                    for (int i = proj.size() - 1; i >= 0; i--) {
                        if (proj.get(i) != null) { last = proj.get(i); break; }
                    }
                    if (last != null) creditAtTarget += last;
                }
            }

            double gross = propertyValue.doubleValue() + savingsAtTarget;
            double net   = gross - creditAtTarget;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", labels[h]);
            row.put("gross", Math.round(gross * 100.0) / 100.0);
            row.put("net",   Math.round(net   * 100.0) / 100.0);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Computes linear regression params [slope, intercept] where x=0 is today.
     */
    private double[] computeRegressionParams(LocalDate today, int trendMonths, List<SavingsAccount> ltAccounts) {
        List<double[]> points = new ArrayList<>();
        for (int i = trendMonths; i >= 0; i--) {
            LocalDate d = today.minusMonths(i);
            double total = 0;
            for (SavingsAccount a : ltAccounts) total += savingsService.projectBalance(a, d).doubleValue();
            // x: months before today (negative), translated so today = trendMonths
            points.add(new double[]{trendMonths - i, total});
        }
        // Use last trendMonths points (starting from index 0 = today - trendMonths)
        int n = points.size();
        if (n < 2) return null;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (double[] p : points) {
            sumX += p[0]; sumY += p[1]; sumXY += p[0] * p[1]; sumX2 += p[0] * p[0];
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        // Translate so x=0 is today (x=trendMonths in the original space)
        // f(x_today + delta) = slope * (trendMonths + delta) + intercept
        // rewrite as: slope * delta + (slope * trendMonths + intercept)
        double newIntercept = slope * trendMonths + intercept;
        return new double[]{slope, newIntercept};
    }

    // ─────────────────── private helpers ───────────────────

    private double[] computeSavingsDepositsProjection(LocalDate chartStart, LocalDate chartEnd, int numMonths) {
        double[] result = new double[numMonths];
        List<SavingsAccount> accounts = getLongTermAccounts();
        LocalDate today = LocalDate.now().withDayOfMonth(1);

        double todayTotal = 0, totalMonthlyDeposits = 0;
        for (SavingsAccount a : accounts) {
            todayTotal += savingsService.projectBalance(a, today).doubleValue();
            totalMonthlyDeposits += a.getMonthlyDeposit().doubleValue();
        }

        LocalDate cursor = chartStart;
        int idx = 0;
        while (!cursor.isAfter(chartEnd.withDayOfMonth(1)) && idx < numMonths) {
            if (!cursor.isAfter(today)) {
                double total = 0;
                for (SavingsAccount a : accounts) total += savingsService.projectBalance(a, cursor).doubleValue();
                result[idx] = total;
            } else {
                long mft = ChronoUnit.MONTHS.between(today, cursor);
                result[idx] = todayTotal + totalMonthlyDeposits * mft;
            }
            cursor = cursor.plusMonths(1);
            idx++;
        }
        return result;
    }

    private double[] computeSavingsTrendProjection(LocalDate chartStart, LocalDate chartEnd, int numMonths, int trendMonths) {
        double[] result = new double[numMonths];
        List<SavingsAccount> accounts = getLongTermAccounts();
        LocalDate today = LocalDate.now().withDayOfMonth(1);

        List<double[]> monthlyTotals = new ArrayList<>();
        LocalDate cursor = chartStart;
        int idx = 0;
        while (!cursor.isAfter(chartEnd.withDayOfMonth(1)) && idx < numMonths) {
            if (!cursor.isAfter(today)) {
                double total = 0;
                for (SavingsAccount a : accounts) total += savingsService.projectBalance(a, cursor).doubleValue();
                result[idx] = total;
                monthlyTotals.add(new double[]{idx, total});
            }
            cursor = cursor.plusMonths(1);
            idx++;
        }

        if (monthlyTotals.size() >= 2) {
            int trendSize = Math.min(trendMonths, monthlyTotals.size());
            List<double[]> trendData = monthlyTotals.subList(monthlyTotals.size() - trendSize, monthlyTotals.size());
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            int n = trendData.size();
            for (double[] p : trendData) {
                sumX += p[0]; sumY += p[1]; sumXY += p[0] * p[1]; sumX2 += p[0] * p[0];
            }
            double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
            double intercept = (sumY - slope * sumX) / n;
            int lastRealIdx = (int) monthlyTotals.get(monthlyTotals.size() - 1)[0];
            for (int i = lastRealIdx + 1; i < numMonths; i++) {
                result[i] = Math.max(0, intercept + slope * i);
            }
        } else if (monthlyTotals.size() == 1) {
            double lastVal = monthlyTotals.get(0)[1];
            int lastRealIdx = (int) monthlyTotals.get(0)[0];
            for (int i = lastRealIdx + 1; i < numMonths; i++) result[i] = lastVal;
        }
        return result;
    }
}
