package com.example.demo.service;

import com.example.demo.model.StatementRow;
import com.example.demo.repository.StatementRowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatementImportService {
    private final StatementCsvReader csvReader;
    private final StatementRowRepository repository;
    private final SavedForecastService savedForecastService;

    public record MonthSummary(int year, int month, long operationCount) { }

    @Transactional
    public long importMonth(MultipartFile file, int year, int month) throws IOException {
        List<StatementRow> rows = csvReader.read(file, year, month);
        repository.deleteByYearAndMonth(year, month);
        repository.flush();
        repository.saveAllAndFlush(rows);
        savedForecastService.invalidateIfAffected(year, month);
        return rows.stream().filter(row -> row.getKind() == StatementRow.Kind.OPERATION).count();
    }

    @Transactional
    public void deleteMonth(int year, int month) {
        if (year < 1900 || year > 2100 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Mois ou année invalide.");
        }
        repository.deleteByYearAndMonth(year, month);
        savedForecastService.invalidateIfAffected(year, month);
    }

    @Transactional(readOnly = true)
    public List<MonthSummary> importedMonths() {
        var counts = new LinkedHashMap<YearMonth, Long>();
        for (StatementRow row : repository.findAllByOrderByYearDescMonthDescLineNumberAsc()) {
            YearMonth period = YearMonth.of(row.getYear(), row.getMonth());
            counts.putIfAbsent(period, 0L);
            if (row.getKind() == StatementRow.Kind.OPERATION) counts.merge(period, 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .map(entry -> new MonthSummary(entry.getKey().getYear(), entry.getKey().getMonthValue(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatementRow> rowsForMonth(int year, int month) {
        return repository.findByYearAndMonthOrderByLineNumberAsc(year, month);
    }
}