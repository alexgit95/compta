package com.example.demo.service;

import com.example.demo.model.StatementRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FixedChargeDetectionService {
    public static final BigDecimal DEFAULT_TOLERANCE_PERCENT = new BigDecimal("4");
    private static final BigDecimal MAX_TOLERANCE_PERCENT = new BigDecimal("10");
    private static final BigDecimal TOLERANCE_STEP = new BigDecimal("0.5");
    private final StatementHistoryService historyService;

    public enum Cadence { MENSUELLE, HEBDOMADAIRE }

    public record Candidate(String label, BigDecimal amount, Cadence cadence, List<LocalDate> dates) {
        public int occurrences() { return dates.size(); }
    }

    public record Analysis(YearMonth from, YearMonth to, List<Candidate> candidates) { }

    public Optional<Analysis> analyze() {
        return analyze(DEFAULT_TOLERANCE_PERCENT);
    }

    public Optional<Analysis> analyze(BigDecimal tolerancePercent) {
        BigDecimal tolerance = toleranceFraction(tolerancePercent);
        return historyService.latestCompleteWindow().map(window ->
                new Analysis(window.from(), window.to(), detectWithTolerance(window.operations(), window.from(), tolerance)));
    }

    public List<Candidate> detect(List<StatementRow> operations, YearMonth firstMonth) {
        return detect(operations, firstMonth, DEFAULT_TOLERANCE_PERCENT);
    }

    public List<Candidate> detect(List<StatementRow> operations, YearMonth firstMonth, BigDecimal tolerancePercent) {
        return detectWithTolerance(operations, firstMonth, toleranceFraction(tolerancePercent));
    }

    public BigDecimal toleranceFraction(BigDecimal tolerancePercent) {
        if (tolerancePercent == null || tolerancePercent.signum() < 0
                || tolerancePercent.compareTo(MAX_TOLERANCE_PERCENT) > 0
                || tolerancePercent.remainder(TOLERANCE_STEP).signum() != 0) {
            throw new IllegalArgumentException("La tolérance doit être comprise entre 0 et 10 % par pas de 0,5.");
        }
        return tolerancePercent.movePointLeft(2);
    }

    private List<Candidate> detectWithTolerance(List<StatementRow> operations, YearMonth firstMonth, BigDecimal tolerance) {
        Map<String, List<StatementRow>> byLabel = operations.stream()
                .filter(row -> row.getKind() == StatementRow.Kind.OPERATION && row.getAmount().signum() < 0)
                .filter(row -> normalizedLabel(row) != null)
                .collect(Collectors.groupingBy(this::normalizedLabel, LinkedHashMap::new, Collectors.toList()));
        List<Candidate> candidates = new ArrayList<>();
        Set<StatementRow> matched = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var entry : byLabel.entrySet()) {
            for (List<StatementRow> group : amountGroups(entry.getValue(), tolerance)) {
                Candidate candidate = monthly(entry.getKey(), group, firstMonth, false);
                if (candidate == null) candidate = weekly(entry.getKey(), group, false);
                if (candidate != null) {
                    candidates.add(candidate);
                    matched.addAll(group);
                }
            }
        }
        List<StatementRow> remainingTransfers = operations.stream()
                .filter(row -> row.getKind() == StatementRow.Kind.OPERATION && row.getAmount().signum() < 0)
                .filter(row -> "Virement".equalsIgnoreCase(row.getTransactionType()))
                .filter(row -> normalizedLabel(row) != null && !matched.contains(row))
                .toList();
        for (List<StatementRow> group : amountGroups(remainingTransfers, tolerance)) {
                String exampleLabel = normalizedLabel(group.stream()
                    .min(Comparator.comparing(StatementRow::getDate).thenComparing(StatementRow::getLineNumber))
                    .orElseThrow());
            String label = group.stream().map(this::normalizedLabel).distinct().count() == 1
                    ? exampleLabel : exampleLabel + " (libellé variable)";
            Candidate candidate = monthly(label, group, firstMonth, true);
            if (candidate == null) candidate = weekly(label, group, true);
            if (candidate != null) candidates.add(candidate);
        }
        return candidates.stream().sorted(Comparator.comparingInt((Candidate candidate) -> candidate.dates().getFirst().getDayOfMonth())
            .thenComparing(Candidate::label)
                .thenComparing(Candidate::amount)).toList();
    }

    private List<List<StatementRow>> amountGroups(List<StatementRow> rows, BigDecimal tolerance) {
        List<List<StatementRow>> groups = new ArrayList<>();
        for (StatementRow row : rows.stream()
                .sorted(Comparator.comparing(value -> value.getAmount().abs())).toList()) {
            if (groups.isEmpty() || !withinTolerance(row.getAmount().abs(), median(groups.getLast()), tolerance)) {
                groups.add(new ArrayList<>());
            }
            groups.getLast().add(row);
        }
        return groups;
    }

    private Candidate monthly(String label, List<StatementRow> group, YearMonth firstMonth, boolean strictTransfer) {
        Map<YearMonth, List<StatementRow>> byMonth = group.stream()
                .collect(Collectors.groupingBy(row -> YearMonth.from(row.getDate())));
        if (strictTransfer && group.size() != 3) return null;
        List<StatementRow> matches = new ArrayList<>();
        List<BigDecimal> monthlyAmounts = new ArrayList<>();
        int occurrencesPerMonth = -1;
        for (int offset = 0; offset < 3; offset++) {
            List<StatementRow> monthRows = byMonth.get(firstMonth.plusMonths(offset));
            if (monthRows == null || monthRows.stream().map(StatementRow::getDate).distinct().count() != 1
                || strictTransfer && monthRows.size() != 1
                || occurrencesPerMonth != -1 && monthRows.size() != occurrencesPerMonth) return null;
            occurrencesPerMonth = monthRows.size();
            matches.addAll(monthRows);
            monthlyAmounts.add(monthRows.stream().map(row -> row.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        int firstDay = matches.stream().mapToInt(row -> row.getDate().getDayOfMonth()).min().orElseThrow();
        int lastDay = matches.stream().mapToInt(row -> row.getDate().getDayOfMonth()).max().orElseThrow();
        if (lastDay - firstDay > 3) return null;
        monthlyAmounts.sort(BigDecimal::compareTo);
        return new Candidate(label, monthlyAmounts.get(1).setScale(2, RoundingMode.HALF_UP), Cadence.MENSUELLE,
            matches.stream().map(StatementRow::getDate).sorted().toList());
    }

    private Candidate weekly(String label, List<StatementRow> group, boolean strictTransfer) {
        List<StatementRow> uniqueDays = group.stream()
                .collect(Collectors.toMap(StatementRow::getDate, row -> row, (first, duplicate) -> first))
                .values().stream().sorted(Comparator.comparing(StatementRow::getDate)).toList();
        if (strictTransfer && uniqueDays.size() != group.size()) return null;
        if (uniqueDays.size() < 3 || uniqueDays.stream().map(row -> YearMonth.from(row.getDate())).distinct().count() < 2) {
            return null;
        }
        for (int index = 1; index < uniqueDays.size(); index++) {
            long days = ChronoUnit.DAYS.between(uniqueDays.get(index - 1).getDate(), uniqueDays.get(index).getDate());
            if (days < 6 || days > 8) return null;
        }
        return candidate(label, uniqueDays, Cadence.HEBDOMADAIRE);
    }

    private Candidate candidate(String label, List<StatementRow> rows, Cadence cadence) {
        return new Candidate(label, median(rows).setScale(2, RoundingMode.HALF_UP), cadence,
                rows.stream().map(StatementRow::getDate).sorted().toList());
    }

    private boolean withinTolerance(BigDecimal amount, BigDecimal reference, BigDecimal tolerance) {
        return amount.subtract(reference).abs().compareTo(reference.multiply(tolerance)) <= 0;
    }

    private BigDecimal median(List<StatementRow> rows) {
        List<BigDecimal> amounts = rows.stream().map(row -> row.getAmount().abs()).sorted().toList();
        int center = amounts.size() / 2;
        return amounts.size() % 2 == 1 ? amounts.get(center)
                : amounts.get(center - 1).add(amounts.get(center)).divide(BigDecimal.valueOf(2));
    }

    private String normalizedLabel(StatementRow row) {
        String raw = row.getDebitLabel() != null && !row.getDebitLabel().isBlank()
                ? row.getDebitLabel() : row.getReference();
        if (raw == null || raw.isBlank()) return null;
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(java.util.Locale.ROOT)
                .replaceAll("\\b\\d{2}/\\d{2}/\\d{2,4}\\b", "")
                .replaceFirst("^(CB|PRLV SEPA|VIR\\.PERMANENT|VIR INST)\\s+", "")
                .replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }
}