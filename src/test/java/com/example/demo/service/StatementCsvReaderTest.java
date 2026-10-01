package com.example.demo.service;

import com.example.demo.model.StatementRow;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StatementCsvReaderTest {
    private final StatementCsvReader reader = new StatementCsvReader();

    private MockMultipartFile csv(String text) {
        return new MockMultipartFile("file", "statement.csv", "text/csv", text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsProvidedStatementWithoutStoringBalances() throws Exception {
        byte[] content = Files.readAllBytes(Path.of("docs/T_cpte_du_01-08-2026_au_31-08-2026.csv"));
        var rows = reader.read(new MockMultipartFile("file", "statement.csv", "text/csv", content), 2026, 8);
        assertTrue(rows.stream().allMatch(row -> row.getKind() == StatementRow.Kind.OPERATION));
        assertEquals(2, rows.get(0).getLineNumber());
        assertTrue(rows.stream().anyMatch(row -> row.getAmount().toPlainString().equals("-250")
                && row.getDebitLabel().contains("Beau voyage")));
        assertTrue(rows.stream().anyMatch(row -> row.getAmount().toPlainString().equals("300")
                && row.getCreditLabel().contains("VIR INST")));
        assertTrue(rows.stream().anyMatch(row -> row.getAmount().toPlainString().equals("-350.76")));
    }

    @Test
    void readsJulyStatementWithFourColumnBalances() throws Exception {
        byte[] content = Files.readAllBytes(Path.of("docs/juillet.csv"));
        var rows = reader.read(new MockMultipartFile("file", "juillet.csv", "text/csv", content), 2026, 7);
        assertFalse(rows.isEmpty());
        assertEquals(2, rows.get(0).getLineNumber());
        assertTrue(rows.stream().allMatch(row -> row.getKind() == StatementRow.Kind.OPERATION));
        assertTrue(rows.stream().anyMatch(row -> row.getDebitLabel().contains("Beau voyage")));
    }

    @Test
    void preservesDuplicateTransactionsAndUtf8Bom() throws Exception {
        var rows = reader.read(csv("\uFEFF01/08/2026;-10,50;Carte;;Achat;;;\n"
                + "01/08/2026;-10,50;Carte;;Achat;;;\n"), 2026, 8);
        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).getLineNumber());
        assertEquals(2, rows.get(1).getLineNumber());
    }

    @Test
    void rejectsWrongMonthAndMalformedRows() {
        assertThrows(IllegalArgumentException.class, () -> reader.read(csv("01/09/2026;-10;Carte;;Achat;;;"), 2026, 8));
        assertThrows(IllegalArgumentException.class, () -> reader.read(csv("01/08/2026;abc;Carte;;Achat;;;"), 2026, 8));
        assertThrows(IllegalArgumentException.class, () -> reader.read(csv("31/02/2026;-10;Carte;;Achat;;;"), 2026, 2));
        assertThrows(IllegalArgumentException.class, () -> reader.read(csv("01/08/2026;-10;Carte;Achat"), 2026, 8));
        assertThrows(IllegalArgumentException.class, () -> reader.read(csv("01/08/2026;-10;Carte;Achat\n01/08/2026;-5;Carte;;Autre;;;"), 2026, 8));
        assertThrows(IllegalArgumentException.class, () -> reader.read(csv("01/08/2026;abc;;A\n01/08/2026;-5;Carte;;Autre;;;"), 2026, 8));
        assertThrows(IllegalArgumentException.class, () -> reader.read(csv("01/08/2026;100;;A"), 2026, 8));
        assertThrows(IllegalArgumentException.class, () -> reader.read(csv("01/08/2026;100;;REF;;;;"), 2026, 8));
        assertThrows(IllegalArgumentException.class, () -> reader.read(csv(""), 2026, 8));
        assertThrows(IllegalArgumentException.class, () -> reader.read(csv("01/08/2026;-10;Carte;;Achat;;;"), 2026, 13));
        assertThrows(IllegalArgumentException.class, () -> reader.read(
            new MockMultipartFile("file", "large.csv", "text/csv", new byte[2 * 1024 * 1024 + 1]), 2026, 8));
    }
}