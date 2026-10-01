package com.example.demo.service;

import com.example.demo.model.StatementRow;
import com.example.demo.repository.StatementRowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatementHistoryService {
    private final StatementRowRepository repository;
    private final Clock clock;

    public record HistoryWindow(YearMonth from, YearMonth to, List<StatementRow> operations) { }

    @Transactional(readOnly = true)
    public Optional<HistoryWindow> latestCompleteWindow() {
        YearMonth currentMonth = YearMonth.now(clock);
        Set<YearMonth> available = repository.findImportedPeriods(StatementRow.Kind.OPERATION).stream()
                .map(period -> YearMonth.of(period.getYear(), period.getMonth()))
                .filter(period -> period.isBefore(currentMonth))
                .collect(Collectors.toSet());
        for (YearMonth last : available.stream().sorted(Comparator.reverseOrder()).toList()) {
            YearMonth first = last.minusMonths(2);
            if (!available.contains(first) || !available.contains(last.minusMonths(1))) continue;
            List<StatementRow> rows = java.util.stream.IntStream.range(0, 3)
                    .mapToObj(first::plusMonths)
                    .flatMap(period -> repository.findByKindAndYearAndMonthOrderByDateAscLineNumberAsc(
                            StatementRow.Kind.OPERATION, period.getYear(), period.getMonthValue()).stream())
                    .sorted(Comparator.comparing(StatementRow::getDate).thenComparing(StatementRow::getLineNumber))
                    .toList();
            return Optional.of(new HistoryWindow(first, last, rows));
        }
        return Optional.empty();
    }
}