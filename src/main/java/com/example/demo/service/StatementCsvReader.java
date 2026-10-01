package com.example.demo.service;

import com.example.demo.model.StatementRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.math.BigDecimal;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatementCsvReader {
    private static final long MAX_SIZE = 2 * 1024 * 1024;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public List<StatementRow> read(MultipartFile file, int year, int month) throws IOException {
        if (year < 1900 || year > 2100 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Mois ou année invalide.");
        }
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Le CSV est vide ou dépasse 2 Mo.");
        }
        YearMonth period = YearMonth.of(year, month);
        List<StatementRow> rows = new ArrayList<>();
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        try (var reader = new PushbackReader(new InputStreamReader(file.getInputStream(), decoder), 1)) {
            int first = reader.read();
            if (first != '\uFEFF' && first != -1) reader.unread(first);
            var format = CSVFormat.DEFAULT.builder().setDelimiter(';').setIgnoreEmptyLines(true).get();
            try (var parser = format.parse(reader)) {
                for (CSVRecord record : parser) {
                    long line = record.getRecordNumber();
                    if (record.size() != 4 && record.size() != 8) {
                        throw new IllegalArgumentException("Ligne " + line + " : 8 colonnes attendues pour une opération.");
                    }
                    try {
                        LocalDate date = LocalDate.parse(record.get(0).trim(), DATE);
                        if (!YearMonth.from(date).equals(period)) {
                            throw new IllegalArgumentException("Date hors du mois sélectionné.");
                        }
                        BigDecimal amount = new BigDecimal(record.get(1).trim().replace(',', '.'));
                        if (amount.scale() > 2 || amount.precision() - amount.scale() > 16) {
                            throw new IllegalArgumentException("Montant hors limites.");
                        }
                        for (int column = 2; column < record.size(); column++) {
                            if (record.get(column).length() > 255) {
                                throw new IllegalArgumentException("Champ texte trop long.");
                            }
                        }
                        boolean balance = record.get(2).isBlank() && !record.get(3).isBlank()
                                && (record.size() == 4 || record.get(4).isBlank() && record.get(5).isBlank()
                                && record.get(6).isBlank() && record.get(7).isBlank());
                        if (record.size() == 4 && !balance) {
                            throw new IllegalArgumentException("8 colonnes attendues pour une opération.");
                        }
                        if (!balance && record.get(2).isBlank() && record.get(4).isBlank()
                                && record.get(5).isBlank()) {
                            throw new IllegalArgumentException("Opération sans type ni libellé.");
                        }
                        if (balance) continue;
                        StatementRow row = new StatementRow();
                        row.setYear(year);
                        row.setMonth(month);
                        row.setLineNumber(line);
                        row.setDate(date);
                        row.setAmount(amount);
                        row.setKind(StatementRow.Kind.OPERATION);
                        row.setTransactionType(record.get(2));
                        row.setReference(record.get(3));
                        row.setDebitLabel(record.get(4));
                        row.setCreditLabel(record.get(5));
                        row.setExtra(record.get(6));
                        row.setCategoryLabel(record.get(7));
                        rows.add(row);
                    } catch (DateTimeException | NumberFormatException ex) {
                        throw new IllegalArgumentException("Ligne " + line + " : date ou montant invalide.", ex);
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException("Ligne " + line + " : " + ex.getMessage(), ex);
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Le relevé ne contient aucune opération.");
        }
        return rows;
    }
}