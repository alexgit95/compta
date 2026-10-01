package com.example.demo.service;

import com.example.demo.model.StatementRow;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FixedChargeDetectionServiceTest {
    private final FixedChargeDetectionService service = new FixedChargeDetectionService(null);

    private StatementRow row(String date, String amount, String label) {
        StatementRow row = new StatementRow();
        row.setDate(LocalDate.parse(date));
        row.setYear(row.getDate().getYear());
        row.setMonth(row.getDate().getMonthValue());
        row.setAmount(new BigDecimal(amount));
        row.setKind(StatementRow.Kind.OPERATION);
        row.setDebitLabel(label);
        return row;
    }

    private StatementRow transfer(String date, String amount, String label) {
        StatementRow row = row(date, amount, label);
        row.setTransactionType("Virement");
        return row;
    }

    @Test
    void detectsMonthlyTransfersWhenTheirLabelsChange() {
        var rows = List.of(
                transfer("2026-08-03", "-300", "VIR.PERMANENT A Boursor"),
            transfer("2026-09-01", "-300", "VIR.PERMANENT AE Boursor"),
            transfer("2026-07-01", "-300", "VIR.PERMANENT Alexandre Boursor"));

        var candidates = service.detect(rows, YearMonth.of(2026, 7));
        assertEquals(1, candidates.size());
        assertEquals(FixedChargeDetectionService.Cadence.MENSUELLE, candidates.get(0).cadence());
        assertEquals(3, candidates.get(0).occurrences());
        assertEquals("ALEXANDRE BOURSOR (libellé variable)", candidates.get(0).label());
    }

        @Test
        void acceptsFourPercentAmountVariationButRejectsMore() {
        var withinTolerance = List.of(
            row("2026-07-03", "-100.00", "PRLV SEPA Abonnement"),
            row("2026-08-03", "-104.00", "PRLV SEPA Abonnement"),
            row("2026-09-03", "-104.00", "PRLV SEPA Abonnement"));
        var outsideTolerance = List.of(
            row("2026-07-03", "-100.00", "PRLV SEPA Abonnement"),
            row("2026-08-03", "-104.01", "PRLV SEPA Abonnement"),
            row("2026-09-03", "-104.01", "PRLV SEPA Abonnement"));

        assertEquals(1, service.detect(withinTolerance, YearMonth.of(2026, 7)).size());
        assertTrue(service.detect(outsideTolerance, YearMonth.of(2026, 7)).isEmpty());
        }

    @Test
    void configurableToleranceAffectsMonthlyGrouping() {
        var rows = List.of(
                row("2026-07-03", "-100.00", "PRLV SEPA Abonnement"),
                row("2026-08-03", "-104.50", "PRLV SEPA Abonnement"),
                row("2026-09-03", "-104.50", "PRLV SEPA Abonnement"));

        assertTrue(service.detect(rows, YearMonth.of(2026, 7), BigDecimal.ZERO).isEmpty());
        assertTrue(service.detect(rows, YearMonth.of(2026, 7), new BigDecimal("4")).isEmpty());
        assertEquals(1, service.detect(rows, YearMonth.of(2026, 7), new BigDecimal("5")).size());
        assertEquals(1, service.detect(rows, YearMonth.of(2026, 7), new BigDecimal("10")).size());
    }

    @Test
    void toleranceMustBeInHalfPointStepsWithinZeroAndTenPercent() {
        assertEquals(0, service.toleranceFraction(BigDecimal.ZERO).compareTo(BigDecimal.ZERO));
        assertEquals(0, service.toleranceFraction(new BigDecimal("0.5")).compareTo(new BigDecimal("0.005")));
        assertEquals(0, service.toleranceFraction(new BigDecimal("10")).compareTo(new BigDecimal("0.10")));
        for (BigDecimal invalid : List.of(new BigDecimal("-0.5"), new BigDecimal("0.1"),
                new BigDecimal("10.5"), new BigDecimal("20"))) {
            assertThrows(IllegalArgumentException.class, () -> service.toleranceFraction(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> service.toleranceFraction(null));
    }

    @Test
    void doesNotGroupDifferentCardLabelsByAmount() {
        var rows = List.of(
                row("2026-07-01", "-300", "CB Shop A"),
                row("2026-08-03", "-300", "CB Shop B"),
                row("2026-09-01", "-300", "CB Shop C"));

        assertTrue(service.detect(rows, YearMonth.of(2026, 7)).isEmpty());
    }

    @Test
    void skipsAmbiguousTransfersOfSameAmountWithinOneMonth() {
        var rows = List.of(
                transfer("2026-07-01", "-300", "VIR.PERMANENT Compte A"),
                transfer("2026-07-02", "-300", "VIR.PERMANENT Compte B"),
                transfer("2026-08-03", "-300", "VIR.PERMANENT Compte C"),
                transfer("2026-09-01", "-300", "VIR.PERMANENT Compte D"));

        assertTrue(service.detect(rows, YearMonth.of(2026, 7)).isEmpty());
    }

    @Test
    void detectsWeeklyTransfersWithChangingLabels() {
        var rows = List.of(
                transfer("2026-07-28", "-40", "VIR INST A"),
                transfer("2026-08-04", "-40", "VIR INST AE"),
                transfer("2026-08-11", "-40", "VIR INST Alexandre"));

        var candidates = service.detect(rows, YearMonth.of(2026, 7));
        assertEquals(1, candidates.size());
        assertEquals(FixedChargeDetectionService.Cadence.HEBDOMADAIRE, candidates.get(0).cadence());
    }

    @Test
    void stableTransferDoesNotProduceDuplicateCandidates() {
        var rows = List.of(
                transfer("2026-07-01", "-250", "VIR.PERMANENT Beau voyage LDDS"),
                transfer("2026-08-01", "-250", "VIR.PERMANENT Beau voyage LDDS"),
                transfer("2026-09-01", "-250", "VIR.PERMANENT Beau voyage LDDS"));

        assertEquals(1, service.detect(rows, YearMonth.of(2026, 7)).size());
    }

    @Test
    void detectsMonthlyExpensesAndSavingsTransfersByRecipientAndAmount() {
        var rows = List.of(
                row("2026-07-03", "-90.00", "PRLV SEPA Navigo Annuel"),
                row("2026-08-04", "-90.80", "PRLV SEPA Navigo Annuel"),
                row("2026-09-03", "-90.80", "PRLV SEPA Navigo Annuel"),
                row("2026-07-01", "-250", "VIR.PERMANENT Beau voyage LDDS"),
                row("2026-08-01", "-250", "VIR.PERMANENT Beau voyage LDDS"),
                row("2026-09-01", "-250", "VIR.PERMANENT Beau voyage LDDS"));

        var candidates = service.detect(rows, YearMonth.of(2026, 7));
        assertEquals(2, candidates.size());
        assertTrue(candidates.stream().allMatch(candidate -> candidate.cadence() == FixedChargeDetectionService.Cadence.MENSUELLE));
        assertEquals(3, candidates.get(0).occurrences());
        assertTrue(candidates.stream().anyMatch(candidate -> candidate.label().contains("BEAU VOYAGE LDDS")
                && candidate.amount().compareTo(new BigDecimal("250.00")) == 0));
    }

        @Test
        void countsTwoMatchingNavigoDebitsPerMonthInTheMonthlyAmount() {
        var rows = List.of(
            row("2026-05-04", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"),
            row("2026-05-04", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"),
            row("2026-06-03", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"),
            row("2026-06-03", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"),
            row("2026-07-03", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"),
            row("2026-07-03", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"));

        var candidates = service.detect(rows, YearMonth.of(2026, 5));
        assertEquals(1, candidates.size());
        assertEquals(6, candidates.get(0).occurrences());
        assertEquals(0, candidates.get(0).amount().compareTo(new BigDecimal("181.60")));
        assertEquals(2, candidates.get(0).dates().stream()
            .filter(date -> YearMonth.from(date).equals(YearMonth.of(2026, 6))).count());
        }

    @Test
    void findsBothNavigoDebitsInTheImportedStatements() throws Exception {
        StatementCsvReader reader = new StatementCsvReader();
        List<StatementRow> operations = new ArrayList<>();
        for (String month : List.of("mai", "juin", "juillet")) {
            int monthNumber = List.of("mai", "juin", "juillet").indexOf(month) + 5;
            operations.addAll(reader.read(new MockMultipartFile("file", month + ".csv", "text/csv",
                    Files.readAllBytes(Path.of("docs", month + ".csv"))), 2026, monthNumber));
        }

        var navigo = service.detect(operations, YearMonth.of(2026, 5)).stream()
                .filter(candidate -> candidate.label().contains("NAVIGO ANNUEL"))
                .toList();
        assertEquals(1, navigo.size());
        assertEquals(6, navigo.getFirst().occurrences());
        assertEquals(0, navigo.getFirst().amount().compareTo(new BigDecimal("181.60")));
    }

        @Test
        void doesNotCountAnIrregularSecondMonthlyDebitAsRecurring() {
        var rows = List.of(
            row("2026-05-04", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"),
            row("2026-05-04", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"),
            row("2026-06-03", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"),
            row("2026-07-03", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"),
            row("2026-07-03", "-90.80", "PRLV SEPA Navigo Annuel - COMUT"));

        assertTrue(service.detect(rows, YearMonth.of(2026, 5)).isEmpty());
        }

        @Test
        void sortsCandidatesByFirstObservedDayOfMonthThenLabel() {
        var rows = List.of(
            row("2026-07-04", "-40", "CB Zulu"), row("2026-08-04", "-40", "CB Zulu"),
            row("2026-09-04", "-40", "CB Zulu"),
            row("2026-07-04", "-20", "CB Beta"), row("2026-08-04", "-20", "CB Beta"),
            row("2026-09-04", "-20", "CB Beta"),
            row("2026-07-12", "-30", "CB Alpha"), row("2026-08-12", "-30", "CB Alpha"),
            row("2026-09-12", "-30", "CB Alpha"),
            row("2026-07-28", "-50", "CB Weekly"), row("2026-08-04", "-50", "CB Weekly"),
            row("2026-08-11", "-50", "CB Weekly"));

        var candidates = service.detect(rows, YearMonth.of(2026, 7));
        assertEquals(List.of("BETA", "ZULU", "ALPHA", "WEEKLY"),
            candidates.stream().map(FixedChargeDetectionService.Candidate::label).toList());
        }

    @Test
    void detectsWeeklySeriesAcrossMonthsOnlyOncePerDay() {
        var rows = List.of(
                row("2026-07-28", "-50", "CB Fitness 27/07/26"),
                row("2026-08-04", "-50", "CB Fitness 03/08/26"),
                row("2026-08-04", "-50", "CB Fitness 03/08/26"),
                row("2026-08-11", "-50", "CB Fitness 10/08/26"),
                row("2026-08-18", "-50", "CB Fitness 17/08/26"));

        var candidates = service.detect(rows, YearMonth.of(2026, 7));
        assertEquals(1, candidates.size());
        assertEquals(FixedChargeDetectionService.Cadence.HEBDOMADAIRE, candidates.get(0).cadence());
        assertEquals(4, candidates.get(0).occurrences());
    }

    @Test
    void normalizesChangingDatesInMortgageLabels() {
        var rows = List.of(
                row("2026-07-05", "-485.57", "PRET IMMOBILIER ECH 05/07/26"),
                row("2026-08-05", "-485.57", "PRET IMMOBILIER ECH 05/08/26"),
                row("2026-09-05", "-485.57", "PRET IMMOBILIER ECH 05/09/26"));

        var candidates = service.detect(rows, YearMonth.of(2026, 7));
        assertEquals(1, candidates.size());
        assertEquals("PRET IMMOBILIER ECH", candidates.get(0).label());
    }

    @Test
    void rejectsCreditsBalancesDifferentPayeesAndIrregularSeries() {
        List<StatementRow> rows = new ArrayList<>(List.of(
                row("2026-07-05", "-30", "PRLV SEPA Alice"),
                row("2026-08-05", "-30", "PRLV SEPA Bob"),
                row("2026-09-05", "-30", "PRLV SEPA Carol"),
                row("2026-07-03", "-50", "CB Sport"),
                row("2026-08-13", "-50", "CB Sport"),
                row("2026-09-03", "-50", "CB Sport"),
                row("2026-08-04", "-20", "CB Cafe"),
                row("2026-08-11", "-20", "CB Cafe"),
                row("2026-08-20", "-20", "CB Cafe"),
                row("2026-07-01", "100", "VIREMENT SALAIRE")));
        StatementRow balance = row("2026-08-01", "-30", "PRLV SEPA Alice");
        balance.setKind(StatementRow.Kind.BALANCE);
        rows.add(balance);

        assertTrue(service.detect(rows, YearMonth.of(2026, 7)).isEmpty());
    }

    @Test
    void duplicateSameDayDoesNotProveMonthlyRecurrence() {
        var rows = List.of(row("2026-07-03", "-10", "CB Shop"),
                row("2026-07-03", "-10", "CB Shop"), row("2026-08-03", "-10", "CB Shop"));

        assertTrue(service.detect(rows, YearMonth.of(2026, 7)).isEmpty());
    }
}