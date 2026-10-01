package com.example.demo.service;

import com.example.demo.model.RecurringExpense;
import com.example.demo.service.FixedChargeDetectionService.Analysis;
import com.example.demo.service.FixedChargeDetectionService.Cadence;
import com.example.demo.service.FixedChargeDetectionService.Candidate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BudgetReconciliationServiceTest {
    private final BudgetReconciliationService service =
            new BudgetReconciliationService(null, new FixedChargeDetectionService(null));

    private Candidate navigo() {
        return new Candidate("NAVIGO ANNUEL - COMUT", new BigDecimal("181.60"), Cadence.MENSUELLE,
                List.of(LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 3),
                        LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 4),
                        LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 3)));
    }

    private RecurringExpense expense(String label, String amount, int day) {
        RecurringExpense expense = new RecurringExpense();
        expense.setLabel(label);
        expense.setAmount(new BigDecimal(amount));
        expense.setDayOfMonth(day);
        return expense;
    }

    @Test
    void findsTwoNavigoPaymentsOrOneAggregatedExpenseRegardlessOfLabel() {
        var two = service.match(navigo(), List.of(expense("Transport A", "90.80", 3),
                expense("Transport B", "90.80", 4)), new BigDecimal("4"));
        assertEquals(BudgetReconciliationService.Status.RETROUVEE, two.status());
        assertEquals(2, two.expenses().size());
        assertEquals(3, two.dayOfMonth());

        var one = service.match(navigo(), List.of(expense("Pass annuel", "181.60", 4)), new BigDecimal("4"));
        assertEquals(BudgetReconciliationService.Status.RETROUVEE, one.status());
        assertEquals(1, one.expenses().size());
    }

    @Test
    void distinguishesPartialCoverageAndAmbiguityFromCompleteMatch() {
        assertEquals(BudgetReconciliationService.Status.A_VERIFIER,
                service.match(navigo(), List.of(expense("Un seul Navigo", "90.80", 3)), new BigDecimal("4")).status());
        assertEquals(BudgetReconciliationService.Status.A_VERIFIER,
                service.match(navigo(), List.of(expense("Total", "181.60", 3),
                        expense("A", "90.80", 3), expense("B", "90.80", 3)), new BigDecimal("4")).status());
        assertEquals(BudgetReconciliationService.Status.A_VERIFIER,
                service.match(navigo(), List.of(expense("A", "90.80", 3), expense("B", "90.80", 3),
                        expense("C", "90.80", 3)), new BigDecimal("4")).status());
    }

    @Test
    void customToleranceAndTwoDayWindowAreAppliedAfterAmount() {
        Candidate candidate = new Candidate("ABONNEMENT", new BigDecimal("100"), Cadence.MENSUELLE,
                List.of(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 5), LocalDate.of(2026, 9, 5)));
        RecurringExpense nearby = expense("Autre libellé", "104.50", 7);

        assertEquals(BudgetReconciliationService.Status.NON_RETROUVEE,
                service.match(candidate, List.of(nearby), new BigDecimal("4")).status());
        assertEquals(BudgetReconciliationService.Status.RETROUVEE,
                service.match(candidate, List.of(nearby), new BigDecimal("5")).status());
        assertEquals(BudgetReconciliationService.Status.A_VERIFIER,
                service.match(candidate, List.of(expense("Autre libellé", "100", 8)), BigDecimal.ZERO).status());
        assertEquals(BudgetReconciliationService.Status.NON_RETROUVEE,
                service.match(candidate, List.of(expense("Autre libellé", "100.01", 5)), BigDecimal.ZERO).status());
    }

    @Test
    void weeklyCandidatesCannotBeComparedAndAreExcludedFromMonthlyTotal() {
        Candidate weekly = new Candidate("COURSES", new BigDecimal("20"), Cadence.HEBDOMADAIRE,
                List.of(LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 11)));
        Candidate monthly = new Candidate("EPARGNE", new BigDecimal("50"), Cadence.MENSUELLE,
                List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)));
        Analysis analysis = new Analysis(YearMonth.of(2026, 7), YearMonth.of(2026, 9),
                List.of(navigo(), weekly, monthly));

        assertEquals(BudgetReconciliationService.Status.NON_COMPARABLE,
                service.match(weekly, List.of(expense("COURSES", "20", 4)), new BigDecimal("4")).status());
        assertEquals(0, service.monthlyTotal(analysis).compareTo(new BigDecimal("231.60")));
        assertEquals(0, service.monthlyTotal(new Analysis(analysis.from(), analysis.to(), List.of(weekly)))
                .compareTo(BigDecimal.ZERO));
    }

    @Test
    void partiallyConfiguredNavigoMustNotBeSavedAsAnAdditionalFullMonthlyTotal() {
        BudgetService budget = mock(BudgetService.class);
        BudgetReconciliationService reconciliation =
                new BudgetReconciliationService(budget, new FixedChargeDetectionService(null));
        when(budget.findAllExpenses()).thenReturn(List.of(expense("Navigo déjà déclaré", "90.80", 3)));

        assertThrows(IllegalStateException.class, () -> reconciliation.saveDetectedExpense(
                navigo(), new BigDecimal("4"), expense("Total Navigo", "181.60", 3)));
        verify(budget, never()).save(any());

        RecurringExpense missingPayment = expense("Second Navigo", "90.80", 3);
        reconciliation.saveDetectedExpense(navigo(), new BigDecimal("4"), missingPayment);
        verify(budget).save(missingPayment);
    }

    @Test
    void reverseListDistinguishesUnmatchedPartialAndConfirmedExpenses() {
        BudgetService budget = mock(BudgetService.class);
        BudgetReconciliationService reconciliation =
                new BudgetReconciliationService(budget, new FixedChargeDetectionService(null));
        RecurringExpense partial = expense("Un Navigo", "90.80", 3);
        RecurringExpense unrelated = expense("Loyer hors relevés", "700", 10);
        when(budget.findAllExpenses()).thenReturn(List.of(partial, unrelated));

        var matches = reconciliation.reconcile(new Analysis(YearMonth.of(2026, 7), YearMonth.of(2026, 9),
                List.of(navigo())), new BigDecimal("4"));
        var unconfirmed = reconciliation.unconfirmed(matches);
        assertEquals(2, unconfirmed.size());
        assertTrue(unconfirmed.get(0).uncertain());
        assertFalse(unconfirmed.get(1).uncertain());

        when(budget.findAllExpenses()).thenReturn(List.of(expense("A", "90.80", 3), expense("B", "90.80", 3)));
        var confirmed = reconciliation.reconcile(new Analysis(YearMonth.of(2026, 7), YearMonth.of(2026, 9),
                List.of(navigo())), new BigDecimal("4"));
        assertTrue(reconciliation.unconfirmed(confirmed).isEmpty());
    }

    @Test
    void competingCandidatesCannotConfirmSameBudgetExpenseTwice() {
        BudgetService budget = mock(BudgetService.class);
        BudgetReconciliationService reconciliation =
                new BudgetReconciliationService(budget, new FixedChargeDetectionService(null));
        RecurringExpense planned = expense("Budget partagé", "50", 3);
        when(budget.findAllExpenses()).thenReturn(List.of(planned));
        Candidate first = new Candidate("Charge A", new BigDecimal("50"), Cadence.MENSUELLE,
                List.of(LocalDate.of(2026, 7, 3), LocalDate.of(2026, 8, 3), LocalDate.of(2026, 9, 3)));
        Candidate second = new Candidate("Charge B", new BigDecimal("50"), Cadence.MENSUELLE, first.dates());

        var matches = reconciliation.reconcile(new Analysis(YearMonth.of(2026, 7), YearMonth.of(2026, 9),
                List.of(first, second)), new BigDecimal("4"));
        assertTrue(matches.stream().allMatch(match -> match.status() == BudgetReconciliationService.Status.A_VERIFIER));
        assertTrue(reconciliation.unconfirmed(matches).getFirst().uncertain());
    }
}