package com.example.demo.service;

import com.example.demo.model.SavedForecastAnalysis;
import com.example.demo.model.SavedForecastCandidate;
import com.example.demo.model.StatementRow;
import com.example.demo.repository.SavedForecastAnalysisRepository;
import com.example.demo.service.FixedChargeDetectionService.Analysis;
import com.example.demo.service.FixedChargeDetectionService.Candidate;
import com.example.demo.service.FixedChargeDetectionService.Cadence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SavedForecastService {
    private static final Long LATEST_ID = 1L;
    private final SavedForecastAnalysisRepository repository;
    private final StatementHistoryService historyService;
    private final Clock clock;

    public record SavedResult(boolean stale, BigDecimal tolerancePercent, Instant analyzedAt, Analysis analysis) { }

    @Transactional
    public Optional<SavedResult> current() {
        return repository.findById(LATEST_ID).map(saved -> {
            if (saved.isValid()) {
                boolean unchanged = historyService.latestCompleteWindow().filter(window ->
                        window.from().equals(saved.from()) && window.to().equals(saved.to())
                                && fingerprint(window.operations()).equals(saved.getSourceFingerprint()))
                        .isPresent();
                if (!unchanged) saved.setValid(false);
            }
            return new SavedResult(!saved.isValid(), saved.getTolerancePercent(), saved.getAnalyzedAt(),
                    saved.isValid() ? toAnalysis(saved) : null);
        });
    }

    @Transactional
    public void save(Analysis analysis, BigDecimal tolerancePercent) {
        var window = historyService.latestCompleteWindow().filter(candidateWindow ->
                candidateWindow.from().equals(analysis.from()) && candidateWindow.to().equals(analysis.to()))
                .orElseThrow(() -> new IllegalStateException("Les relevés ont changé pendant l'analyse."));
        SavedForecastAnalysis saved = repository.findById(LATEST_ID).orElseGet(() -> {
            SavedForecastAnalysis next = new SavedForecastAnalysis();
            next.setId(LATEST_ID);
            return next;
        });
        saved.setFromYear(analysis.from().getYear());
        saved.setFromMonth(analysis.from().getMonthValue());
        saved.setToYear(analysis.to().getYear());
        saved.setToMonth(analysis.to().getMonthValue());
        saved.setTolerancePercent(tolerancePercent);
        saved.setAnalyzedAt(Instant.now(clock));
        saved.setSourceFingerprint(fingerprint(window.operations()));
        saved.setValid(true);
        saved.getCandidates().clear();
        for (Candidate candidate : analysis.candidates()) {
            SavedForecastCandidate row = new SavedForecastCandidate();
            row.setLabel(candidate.label());
            row.setAmount(candidate.amount());
            row.setCadence(candidate.cadence().name());
            row.getDates().addAll(candidate.dates());
            saved.getCandidates().add(row);
        }
        repository.saveAndFlush(saved);
    }

    @Transactional
    public void invalidateIfAffected(int year, int month) {
        YearMonth period = YearMonth.of(year, month);
        repository.findById(LATEST_ID).ifPresent(saved -> {
            if (!period.isBefore(saved.from()) && !period.isAfter(saved.to())) saved.setValid(false);
        });
    }

    private Analysis toAnalysis(SavedForecastAnalysis saved) {
        List<Candidate> candidates = saved.getCandidates().stream()
                .map(row -> new Candidate(row.getLabel(), row.getAmount(), Cadence.valueOf(row.getCadence()),
                        List.copyOf(row.getDates())))
                .toList();
        return new Analysis(saved.from(), saved.to(), candidates);
    }

    private String fingerprint(List<StatementRow> rows) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (StatementRow row : rows) {
                update(digest, row.getDate().toString());
                update(digest, row.getAmount().stripTrailingZeros().toPlainString());
                update(digest, Long.toString(row.getLineNumber()));
                update(digest, row.getTransactionType());
                update(digest, row.getReference());
                update(digest, row.getDebitLabel());
                update(digest, row.getCreditLabel());
                update(digest, row.getExtra());
                update(digest, row.getCategoryLabel());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponible.", ex);
        }
    }

    private void update(MessageDigest digest, String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}