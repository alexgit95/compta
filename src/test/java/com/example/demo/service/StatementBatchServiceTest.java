package com.example.demo.service;

import com.example.demo.dto.StatementBatchForm.StatementUpload;
import com.example.demo.model.StatementRow;
import com.example.demo.repository.StatementRowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statement-batches;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class StatementBatchServiceTest {
    @Autowired private StatementBatchService batchService;
    @Autowired private StatementRowRepository repository;

    @BeforeEach
    void clearRows() {
        repository.deleteAllInBatch();
    }

    private StatementUpload upload(int year, int month, String csv) {
        StatementUpload upload = new StatementUpload();
        upload.setYear(year);
        upload.setMonth(month);
        upload.setFile(new MockMultipartFile("file", "statement.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)));
        return upload;
    }

    @Test
    void validMonthsCommitDespiteOneInvalidCsv() {
        var results = batchService.importBatch(List.of(
                upload(2026, 8, "01/08/2026;-10;Carte;;Achat;;;\n"),
                upload(2026, 9, "01/08/2026;-20;Carte;;Mauvais mois;;;\n"),
                upload(2026, 10, "01/10/2026;-30;Carte;;Autre;;;\n")));

        assertEquals(List.of(true, false, true), results.stream().map(StatementBatchService.UploadResult::success).toList());
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).size());
        assertTrue(repository.findByYearAndMonthOrderByLineNumberAsc(2026, 9).isEmpty());
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 10).size());
    }

    @Test
    void duplicateMonthFailsBothRowsAndPreservesPreviousData() {
        batchService.importBatch(List.of(upload(2026, 8, "01/08/2026;-10;Carte;;Avant;;;\n")));
        var results = batchService.importBatch(List.of(
                upload(2026, 8, "01/08/2026;-20;Carte;;Après;;;\n"),
                upload(2026, 9, "01/09/2026;-30;Carte;;Autre;;;\n"),
                upload(2026, 8, "01/08/2026;-40;Carte;;Double;;;\n")));

        assertEquals(List.of(false, true, false), results.stream().map(StatementBatchService.UploadResult::success).toList());
        assertEquals("Avant", repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).get(0).getDebitLabel());
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 9).size());
    }

    @Test
    void rejectsEmptyAndOversizedBatchWithoutWriting() {
        assertThrows(IllegalArgumentException.class, () -> batchService.importBatch(List.of()));
        var eleven = new ArrayList<StatementUpload>();
        for (int index = 0; index < 11; index++) {
            eleven.add(upload(2026, 1 + index, "01/01/2026;-10;Carte;;Achat;;;\n"));
        }
        assertThrows(IllegalArgumentException.class, () -> batchService.importBatch(eleven));
        assertEquals(0, repository.count());
    }

    @Test
    void oversizedFileFailsOnlyItsMonth() {
        StatementUpload large = upload(2026, 8, "01/08/2026;-10;Carte;;Achat;;;\n");
        large.setFile(new MockMultipartFile("file", "large.csv", "text/csv", new byte[2 * 1024 * 1024 + 1]));
        var results = batchService.importBatch(List.of(large,
                upload(2026, 9, "01/09/2026;-20;Carte;;Autre;;;\n")));

        assertFalse(results.get(0).success());
        assertTrue(results.get(0).error().contains("2 Mo"));
        assertTrue(results.get(1).success());
        assertTrue(repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).isEmpty());
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 9).size());
    }

    @Test
    void importsJulyFileAlongsideAnotherMonthWithoutBalances() throws Exception {
        StatementUpload july = upload(2026, 7, "");
        july.setFile(new MockMultipartFile("file", "juillet.csv", "text/csv",
                Files.readAllBytes(Path.of("docs/juillet.csv"))));
        var results = batchService.importBatch(List.of(july,
                upload(2026, 8, "01/08/2026;-10;Carte;;Achat;;;\n")));

        assertEquals(List.of(true, true), results.stream().map(StatementBatchService.UploadResult::success).toList());
        var julyRows = repository.findByYearAndMonthOrderByLineNumberAsc(2026, 7);
        assertEquals(results.get(0).operationCount(), julyRows.size());
        assertTrue(julyRows.stream().allMatch(row -> row.getKind() == StatementRow.Kind.OPERATION));
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).size());
    }
}