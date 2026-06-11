package com.example.demo.service;

import com.example.demo.model.Credit;
import com.example.demo.repository.CreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRepository creditRepository;

    public List<Credit> findAll() {
        return creditRepository.findAllByOrderByEndDateAsc();
    }

    public Credit findById(Long id) {
        return creditRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Credit not found: " + id));
    }

    @Transactional
    public Credit save(Credit credit) {
        return creditRepository.save(credit);
    }

    @Transactional
    public void delete(Long id) {
        creditRepository.deleteById(id);
    }

    /**
     * Calculates the repayment percentage based on remaining amount vs total amount.
     */
    public BigDecimal getRepaymentPercentage(Credit credit) {
        return getRepaymentPercentage(credit, credit.getRemainingAmount());
    }

    /**
     * Calculates the repayment percentage based on a provided remaining amount.
     */
    public BigDecimal getRepaymentPercentage(Credit credit, BigDecimal remainingAmount) {
        if (credit.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        BigDecimal paid = credit.getTotalAmount().subtract(remainingAmount);
        return paid.multiply(BigDecimal.valueOf(100))
                .divide(credit.getTotalAmount(), 1, RoundingMode.HALF_UP);
    }

    /**
     * Computes the current remaining amount by subtracting elapsed monthly payments
     * since the reference date of the stored remaining amount.
     */
    public BigDecimal getCurrentRemainingAmount(Credit credit) {
        return getCurrentRemainingAmount(credit, LocalDate.now());
    }

    public BigDecimal getCurrentRemainingAmount(Credit credit, LocalDate asOfDate) {
        LocalDate refMonth = credit.getRemainingAmountDate().withDayOfMonth(1);
        LocalDate asOfMonth = asOfDate.withDayOfMonth(1);

        long elapsedMonths = asOfMonth.isAfter(refMonth)
                ? ChronoUnit.MONTHS.between(refMonth, asOfMonth)
                : 0;

        BigDecimal paidSinceReference = credit.getMonthlyPayment().multiply(BigDecimal.valueOf(elapsedMonths));
        BigDecimal currentRemaining = credit.getRemainingAmount().subtract(paidSinceReference);

        return currentRemaining.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates remaining duration in months from today to end date.
     */
    public long getRemainingMonths(Credit credit) {
        LocalDate today = LocalDate.now();
        if (today.isAfter(credit.getEndDate())) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(today, credit.getEndDate());
    }

    /**
     * Returns remaining duration as "X an(s) Y mois" string.
     */
    public String getRemainingDurationLabel(Credit credit) {
        long months = getRemainingMonths(credit);
        long years = months / 12;
        long remainingMonths = months % 12;
        if (years == 0) {
            return remainingMonths + " mois";
        }
        if (remainingMonths == 0) {
            return years + " an" + (years > 1 ? "s" : "");
        }
        return years + " an" + (years > 1 ? "s" : "") + " " + remainingMonths + " mois";
    }

    /**
     * Projects the remaining capital month by month using standard amortization.
     * Returns one value per month from fromMonth to toMonth (inclusive).
     */
    public List<Double> projectMonthlyRemaining(Credit credit, LocalDate fromMonth, LocalDate toMonth) {
        List<Double> result = new ArrayList<>();
        BigDecimal monthlyRate = credit.getRate().compareTo(BigDecimal.ZERO) > 0
                ? credit.getRate().divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LocalDate refMonth = credit.getRemainingAmountDate().withDayOfMonth(1);
        LocalDate from = fromMonth.withDayOfMonth(1);
        LocalDate to = toMonth.withDayOfMonth(1);

        BigDecimal remaining = credit.getRemainingAmount();

        // Fast-forward amortization from refMonth to fromMonth
        LocalDate temp = refMonth;
        while (temp.isBefore(from) && remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principal = credit.getMonthlyPayment().subtract(interest);
            remaining = remaining.subtract(principal).max(BigDecimal.ZERO);
            temp = temp.plusMonths(1);
        }

        // Generate monthly data — null once the credit is done (Chart.js will stop drawing the line)
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            if (cursor.isAfter(credit.getEndDate().withDayOfMonth(1)) || remaining.compareTo(BigDecimal.ZERO) <= 0) {
                result.add(null);
            } else {
                double val = remaining.setScale(2, RoundingMode.HALF_UP).doubleValue();
                result.add(val);
                BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal principal = credit.getMonthlyPayment().subtract(interest);
                remaining = remaining.subtract(principal).max(BigDecimal.ZERO);
            }
            cursor = cursor.plusMonths(1);
        }
        return result;
    }
}
