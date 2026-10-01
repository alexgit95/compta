package com.example.demo.service;

import com.example.demo.model.StatementRow;
import com.example.demo.repository.SavedForecastAnalysisRepository;
import com.example.demo.repository.StatementRowRepository;
import com.example.demo.service.FixedChargeDetectionService.Analysis;
import com.example.demo.service.FixedChargeDetectionService.Cadence;
import com.example.demo.service.FixedChargeDetectionService.Candidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:saved-analysis;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class SavedForecastServiceTest {
    @TestConfiguration
    static class FixedClockConfig {
        @Bean @Primary
        Clock savedAnalysisClock() {
            return new MutableClock(Instant.parse("2026-11-01T12:00:00Z"), ZoneId.of("Europe/Paris"));
        }
    }

    private static class MutableClock extends Clock {
        private volatile Instant current;
        private final ZoneId zone;

        MutableClock(Instant current, ZoneId zone) {
            this.current = current;
            this.zone = zone;
        }

        void moveTo(Instant instant) { current = instant; }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId newZone) { return new MutableClock(current, newZone); }
        @Override public Instant instant() { return current; }
    }

    @Autowired private Clock clock;
    @Autowired private SavedForecastService savedService;
    @Autowired private SavedForecastAnalysisRepository savedRepository;
    @Autowired private StatementRowRepository statementRepository;
    @Autowired private StatementImportService importService;

    @BeforeEach
    void setup() {
        ((MutableClock) clock).moveTo(Instant.parse("2026-11-01T12:00:00Z"));
        savedRepository.findById(1L).ifPresent(savedRepository::delete);
        savedRepository.flush();
        statementRepository.deleteAllInBatch();
    }

    private void store(int month, int line) {
        StatementRow row = new StatementRow();
        row.setYear(2026);
        row.setMonth(month);
        row.setLineNumber(line);
        row.setDate(LocalDate.of(2026, month, 3));
        row.setAmount(new BigDecimal("-90.80"));
        row.setDebitLabel("PRLV SEPA Navigo Annuel");
        row.setKind(StatementRow.Kind.OPERATION);
        statementRepository.saveAndFlush(row);
    }

    private Analysis analysis() {
        Candidate navigo = new Candidate("NAVIGO ANNUEL", new BigDecimal("181.60"), Cadence.MENSUELLE,
                List.of(LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 3),
                        LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3),
                        LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 3)));
        return new Analysis(YearMonth.of(2026, 7), YearMonth.of(2026, 9), List.of(navigo));
    }

    private void seed() {
        for (int month : new int[]{7, 8, 9}) {
            store(month, 1);
            store(month, 2);
        }
    }

    @Test
    void savesLatestAnalysisWithEveryDuplicateOccurrenceAndReplacesIt() {
        assertTrue(savedService.current().isEmpty());
        seed();
        savedService.save(analysis(), new BigDecimal("5"));

        var stored = savedService.current().orElseThrow();
        assertFalse(stored.stale());
        assertEquals(new BigDecimal("5.0"), stored.tolerancePercent());
        assertEquals(6, stored.analysis().candidates().getFirst().occurrences());
        assertEquals(stored.analysis().candidates().getFirst().dates().get(0),
                stored.analysis().candidates().getFirst().dates().get(1));

        savedService.save(analysis(), new BigDecimal("4"));
        assertEquals(1, savedRepository.count());
        assertEquals(0, savedService.current().orElseThrow().tolerancePercent().compareTo(new BigDecimal("4")));
    }

    @Test
    void detectsChangedStatementsAndNewerEligibleWindowWithoutRerunningAnalysis() {
        seed();
        savedService.save(analysis(), new BigDecimal("4"));
        StatementRow changed = statementRepository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).getFirst();
        changed.setDebitLabel("Autre libellé");
        statementRepository.saveAndFlush(changed);
        assertTrue(savedService.current().orElseThrow().stale());

        savedService.save(analysis(), new BigDecimal("4"));
        store(10, 1);
        assertTrue(savedService.current().orElseThrow().stale());
    }

    @Test
    void failedReplacementDoesNotErasePreviousResult() {
        seed();
        savedService.save(analysis(), new BigDecimal("4"));
        Analysis differentPeriod = new Analysis(YearMonth.of(2026, 6), YearMonth.of(2026, 8), List.of());
        assertThrows(IllegalStateException.class, () -> savedService.save(differentPeriod, BigDecimal.ZERO));
        assertEquals(6, savedService.current().orElseThrow().analysis().candidates().getFirst().occurrences());
    }

    @Test
    void invalidAndOlderImportsKeepSnapshotButDeletingAnalyzedMonthInvalidatesIt() throws Exception {
        seed();
        savedService.save(analysis(), new BigDecimal("4"));
        MockMultipartFile invalid = new MockMultipartFile("file", "wrong.csv", "text/csv",
                "01/05/2026;-10;Carte;;Achat;;;".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> importService.importMonth(invalid, 2026, 8));
        assertFalse(savedService.current().orElseThrow().stale());

        importService.importMonth(invalid, 2026, 5);
        assertFalse(savedService.current().orElseThrow().stale());

        importService.deleteMonth(2026, 8);
        assertTrue(savedService.current().orElseThrow().stale());
    }

    @Test
    void monthRolloverMakesNewlyCompletedStatementWindowStale() {
        ((MutableClock) clock).moveTo(Instant.parse("2026-10-01T12:00:00Z"));
        seed();
        savedService.save(analysis(), new BigDecimal("4"));
        store(10, 1);
        assertFalse(savedService.current().orElseThrow().stale());

        ((MutableClock) clock).moveTo(Instant.parse("2026-11-01T12:00:00Z"));
        assertTrue(savedService.current().orElseThrow().stale());
    }
}