package com.example.demo.service;

import com.example.demo.model.StatementRow;
import com.example.demo.repository.StatementRowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statements;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Transactional
class StatementImportServiceTest {
    @Autowired private StatementImportService service;
    @Autowired private StatementRowRepository repository;

    private MockMultipartFile csv(String text) {
        return new MockMultipartFile("file", "statement.csv", "text/csv", text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void replacesOnlySelectedMonthAndCountsOperations() throws Exception {
        service.importMonth(csv("01/08/2026;100;;REF;;;;\n01/08/2026;-10;Carte;;Achat;;;\n"), 2026, 8);
        service.importMonth(csv("01/09/2026;-20;Carte;;Autre;;;\n"), 2026, 9);
        long count = service.importMonth(csv("02/08/2026;-30;Carte;;Nouveau;;;\n"
                + "02/08/2026;-30;Carte;;Nouveau;;;\n"), 2026, 8);

        assertEquals(2, count);
        assertEquals(2, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).size());
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 9).size());
        assertEquals(2, service.importedMonths().get(1).operationCount());
    }

    @Test
    void invalidReplacementKeepsOriginalMonth() throws Exception {
        service.importMonth(csv("01/08/2026;-10;Carte;;Achat;;;\n"), 2026, 8);
        assertThrows(IllegalArgumentException.class, () -> service.importMonth(
                csv("01/08/2026;-20;Carte;;Achat;;;\n01/09/2026;-30;Carte;;Autre;;;\n"), 2026, 8));
        var original = repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8);
        assertEquals(1, original.size());
        assertEquals(StatementRow.Kind.OPERATION, original.get(0).getKind());
        assertEquals("-10", original.get(0).getAmount().toPlainString());
    }

    @Test
    void keepsOldBalancesUntilTheirMonthIsReimported() throws Exception {
        StatementRow previousBalance = new StatementRow();
        previousBalance.setYear(2026);
        previousBalance.setMonth(8);
        previousBalance.setLineNumber(1);
        previousBalance.setDate(LocalDate.of(2026, 8, 1));
        previousBalance.setAmount(new BigDecimal("500"));
        previousBalance.setKind(StatementRow.Kind.BALANCE);
        repository.saveAndFlush(previousBalance);

        service.importMonth(csv("01/09/2026;-20;Carte;;Autre;;;\n"), 2026, 9);
        assertEquals(StatementRow.Kind.BALANCE,
                repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).get(0).getKind());

        service.importMonth(csv("01/08/2026;500;;REF;;;;\n02/08/2026;-10;Carte;;Achat;;;\n"), 2026, 8);
        var august = repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8);
        assertEquals(1, august.size());
        assertEquals(StatementRow.Kind.OPERATION, august.get(0).getKind());
        assertEquals(2, august.get(0).getLineNumber());
    }
}