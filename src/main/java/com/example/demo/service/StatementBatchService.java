package com.example.demo.service;

import com.example.demo.dto.StatementBatchForm.StatementUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatementBatchService {
    private final StatementImportService statementImportService;

    public record UploadResult(int position, Integer year, Integer month, boolean success,
                               long operationCount, String error) { }

    public List<UploadResult> importBatch(List<StatementUpload> uploads) {
        if (uploads == null || uploads.isEmpty() || uploads.size() > 10) {
            throw new IllegalArgumentException("Sélectionnez entre 1 et 10 relevés.");
        }
        Map<YearMonth, Integer> occurrences = new HashMap<>();
        for (StatementUpload upload : uploads) {
            YearMonth period = periodOf(upload);
            if (period != null) occurrences.merge(period, 1, Integer::sum);
        }
        java.util.ArrayList<UploadResult> results = new java.util.ArrayList<>();
        for (int index = 0; index < uploads.size(); index++) {
            StatementUpload upload = uploads.get(index);
            Integer year = upload == null ? null : upload.getYear();
            Integer month = upload == null ? null : upload.getMonth();
            YearMonth period = periodOf(upload);
            if (period == null) {
                results.add(new UploadResult(index + 1, year, month, false, 0, "Mois ou année invalide."));
            } else if (occurrences.get(period) > 1) {
                results.add(new UploadResult(index + 1, year, month, false, 0, "Mois présent plusieurs fois dans le lot."));
            } else {
                try {
                    long count = statementImportService.importMonth(upload.getFile(), year, month);
                    results.add(new UploadResult(index + 1, year, month, true, count, null));
                } catch (IllegalArgumentException | IOException ex) {
                    results.add(new UploadResult(index + 1, year, month, false, 0, ex.getMessage()));
                } catch (RuntimeException ex) {
                    results.add(new UploadResult(index + 1, year, month, false, 0, "Erreur lors de l'enregistrement du mois."));
                }
            }
        }
        return results;
    }

    private YearMonth periodOf(StatementUpload upload) {
        if (upload == null || upload.getYear() == null || upload.getMonth() == null
                || upload.getYear() < 1900 || upload.getYear() > 2100) return null;
        try {
            return YearMonth.of(upload.getYear(), upload.getMonth());
        } catch (DateTimeException ex) {
            return null;
        }
    }
}