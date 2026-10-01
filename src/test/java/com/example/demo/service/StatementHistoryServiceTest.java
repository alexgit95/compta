package com.example.demo.service;

import com.example.demo.model.StatementRow;
import com.example.demo.repository.StatementRowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:history-selection;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class StatementHistoryServiceTest {
    @Autowired private StatementRowRepository repository;

    @BeforeEach
    void clearRows() {
        repository.deleteAllInBatch();
    }

    private StatementHistoryService at(String date) {
        Clock clock = Clock.fixed(Instant.parse(date + "T12:00:00Z"), ZoneId.of("Europe/Paris"));
        return new StatementHistoryService(repository, clock);
    }

    private void store(int year, int month, StatementRow.Kind kind) {
        StatementRow row = new StatementRow();
        row.setYear(year);
        row.setMonth(month);
        row.setLineNumber(1);
        row.setDate(LocalDate.of(year, month, 1));
        row.setAmount(new BigDecimal("-10"));
        row.setKind(kind);
        repository.saveAndFlush(row);
    }

    @Test
    void fallsBackToLatestConsecutiveRunAndIgnoresCurrentMonth() {
        for (int month : new int[]{5, 6, 7, 9, 10}) store(2026, month, StatementRow.Kind.OPERATION);

        var window = at("2026-10-01").latestCompleteWindow().orElseThrow();
        assertEquals(java.time.YearMonth.of(2026, 5), window.from());
        assertEquals(java.time.YearMonth.of(2026, 7), window.to());
        assertEquals(3, window.operations().size());
    }

    @Test
    void supportsYearBoundaryAndDoesNotCountBalanceOnlyMonths() {
        store(2025, 11, StatementRow.Kind.OPERATION);
        store(2025, 12, StatementRow.Kind.OPERATION);
        store(2026, 1, StatementRow.Kind.OPERATION);
        store(2026, 2, StatementRow.Kind.BALANCE);

        var window = at("2026-03-01").latestCompleteWindow().orElseThrow();
        assertEquals(java.time.YearMonth.of(2025, 11), window.from());
        assertEquals(java.time.YearMonth.of(2026, 1), window.to());
        assertTrue(window.operations().stream().allMatch(row -> row.getKind() == StatementRow.Kind.OPERATION));
    }

    @Test
    void reportsInsufficientHistoryForSeparatedOrFutureMonths() {
        store(2026, 6, StatementRow.Kind.OPERATION);
        store(2026, 8, StatementRow.Kind.OPERATION);
        store(2026, 9, StatementRow.Kind.OPERATION);
        store(2026, 10, StatementRow.Kind.OPERATION);

        assertTrue(at("2026-10-01").latestCompleteWindow().isEmpty());
    }
}