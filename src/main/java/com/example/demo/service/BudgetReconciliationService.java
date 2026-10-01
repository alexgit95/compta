package com.example.demo.service;

import com.example.demo.model.RecurringExpense;
import com.example.demo.service.FixedChargeDetectionService.Analysis;
import com.example.demo.service.FixedChargeDetectionService.Candidate;
import com.example.demo.service.FixedChargeDetectionService.Cadence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetReconciliationService {
    private final BudgetService budgetService;
    private final FixedChargeDetectionService detectionService;

    public enum Status { RETROUVEE, A_VERIFIER, NON_RETROUVEE, NON_COMPARABLE }

    public record Match(Candidate candidate, Status status, List<RecurringExpense> expenses, int dayOfMonth) { }
    public record Unconfirmed(RecurringExpense expense, boolean uncertain) { }

    @Transactional(readOnly = true)
    public List<Match> reconcile(Analysis analysis, BigDecimal tolerancePercent) {
        List<RecurringExpense> expenses = budgetService.findAllExpenses();
        List<Match> matches = analysis.candidates().stream()
            .map(candidate -> match(candidate, expenses, tolerancePercent)).toList();
        return matches.stream().map(result -> {
            if (result.status() != Status.RETROUVEE || result.expenses().stream().noneMatch(expense ->
                matches.stream().filter(other -> other != result)
                    .anyMatch(other -> other.expenses().stream().anyMatch(candidate -> sameExpense(expense, candidate))))) {
            return result;
            }
            return new Match(result.candidate(), Status.A_VERIFIER, result.expenses(), result.dayOfMonth());
        }).toList();
    }

        @Transactional(readOnly = true)
        public List<Unconfirmed> unconfirmed(List<Match> matches) {
        return budgetService.findAllExpenses().stream().filter(expense ->
            matches.stream().filter(result -> result.status() == Status.RETROUVEE)
                .noneMatch(result -> result.expenses().stream().anyMatch(candidate -> sameExpense(expense, candidate))))
            .map(expense -> new Unconfirmed(expense, matches.stream()
                .filter(result -> result.status() == Status.A_VERIFIER)
                .anyMatch(result -> result.expenses().stream().anyMatch(candidate -> sameExpense(expense, candidate)))))
            .toList();
        }

        private boolean sameExpense(RecurringExpense first, RecurringExpense second) {
        return first == second || first.getId() != null && first.getId().equals(second.getId());
        }

    public BigDecimal monthlyTotal(Analysis analysis) {
        return analysis.candidates().stream().filter(candidate -> candidate.cadence() == Cadence.MENSUELLE)
                .map(Candidate::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Match match(Candidate candidate, List<RecurringExpense> expenses, BigDecimal tolerancePercent) {
        if (candidate.cadence() == Cadence.HEBDOMADAIRE) {
            return new Match(candidate, Status.NON_COMPARABLE, List.of(), 0);
        }
        BigDecimal tolerance = detectionService.toleranceFraction(tolerancePercent);
        int day = dayOfMonth(candidate);
        int paymentsPerMonth = candidate.occurrences() / 3;
        BigDecimal unitAmount = candidate.amount().divide(BigDecimal.valueOf(paymentsPerMonth), 8, RoundingMode.HALF_UP);

        List<RecurringExpense> fullAmount = expenses.stream()
                .filter(expense -> closeTo(expense.getAmount(), candidate.amount(), tolerance)).toList();
        List<RecurringExpense> unitAmounts = paymentsPerMonth > 1 ? expenses.stream()
                .filter(expense -> closeTo(expense.getAmount(), unitAmount, tolerance)).toList() : List.of();

        List<List<RecurringExpense>> complete = new ArrayList<>();
        fullAmount.stream().filter(expense -> closeDay(expense, day))
                .forEach(expense -> complete.add(List.of(expense)));
        List<RecurringExpense> unitAtDay = unitAmounts.stream().filter(expense -> closeDay(expense, day)).toList();
        if (unitAtDay.size() == paymentsPerMonth
                && closeTo(unitAtDay.stream().map(RecurringExpense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add), candidate.amount(), tolerance)) {
            complete.add(unitAtDay);
        }

        if (complete.size() == 1 && unitAtDay.size() <= paymentsPerMonth) {
            return new Match(candidate, Status.RETROUVEE, complete.getFirst(), day);
        }
        LinkedHashSet<RecurringExpense> plausible = new LinkedHashSet<>(fullAmount);
        plausible.addAll(unitAmounts);
        if (!plausible.isEmpty()) {
            return new Match(candidate, Status.A_VERIFIER, List.copyOf(plausible), day);
        }
        return new Match(candidate, Status.NON_RETROUVEE, List.of(), day);
    }

    private boolean closeTo(BigDecimal actual, BigDecimal expected, BigDecimal tolerance) {
        return actual.subtract(expected).abs().compareTo(expected.multiply(tolerance)) <= 0;
    }

    private boolean closeDay(RecurringExpense expense, int day) {
        return Math.abs(expense.getDayOfMonth() - day) <= 2;
    }

    public int dayOfMonth(Candidate candidate) {
        List<Integer> days = candidate.dates().stream().map(date -> date.getDayOfMonth()).sorted().toList();
        int center = days.size() / 2;
        return days.size() % 2 == 1 ? days.get(center)
                : BigDecimal.valueOf(days.get(center - 1) + days.get(center))
                        .divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP).intValueExact();
    }

    @Transactional
    public void saveDetectedExpense(Candidate candidate, BigDecimal tolerancePercent, RecurringExpense expense) {
        if (candidate.cadence() != Cadence.MENSUELLE || expense.getId() != null) {
            throw new IllegalArgumentException("Seule une nouvelle charge mensuelle peut être ajoutée.");
        }
        if (expense.getLabel() == null || expense.getLabel().isBlank() || expense.getAmount() == null
                || expense.getAmount().signum() <= 0 || expense.getDayOfMonth() < 1 || expense.getDayOfMonth() > 31) {
            throw new IllegalArgumentException("Vérifiez le libellé, le montant et le jour de la dépense.");
        }
        BigDecimal tolerance = detectionService.toleranceFraction(tolerancePercent);
        List<RecurringExpense> existing = budgetService.findAllExpenses();
        if (match(candidate, existing, tolerancePercent).status() == Status.RETROUVEE) {
            throw new IllegalStateException("Une dépense Budget de montant et de jour proches existe déjà.");
        }
        int requiredPayments = candidate.occurrences() / 3;
        BigDecimal unitAmount = candidate.amount().divide(BigDecimal.valueOf(requiredPayments), 8, RoundingMode.HALF_UP);
        long existingPayments = existing.stream().filter(current -> closeDay(current, dayOfMonth(candidate))
            && closeTo(current.getAmount(), unitAmount, tolerance)).count();
        if (requiredPayments > 1 && existingPayments > 0
            && closeTo(expense.getAmount(), candidate.amount(), tolerance)
            && closeDay(expense, dayOfMonth(candidate))) {
            throw new IllegalStateException("Une partie de cette charge est déjà prévue dans Budget ; ajustez le montant avant l'ajout.");
        }
        boolean completingMultiplePayments = requiredPayments > 1 && existingPayments < requiredPayments
            && closeTo(expense.getAmount(), unitAmount, tolerance)
            && closeDay(expense, dayOfMonth(candidate));
        if (!completingMultiplePayments && existing.stream().anyMatch(current ->
            closeDay(current, expense.getDayOfMonth())
                && closeTo(current.getAmount(), expense.getAmount(), tolerance))) {
            throw new IllegalStateException("Une dépense Budget de montant et de jour proches existe déjà.");
        }
        budgetService.save(expense);
    }
}