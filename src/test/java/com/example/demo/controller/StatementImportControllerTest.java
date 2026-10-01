package com.example.demo.controller;

import com.example.demo.model.StatementRow;
import com.example.demo.repository.StatementRowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statement-web;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Transactional
class StatementImportControllerTest {
    @Autowired private WebApplicationContext context;
    @Autowired private StatementRowRepository repository;
    @Autowired private Environment environment;
    @Autowired private AdminUploadExceptionHandler uploadExceptionHandler;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    private MockMultipartFile csv() {
        return new MockMultipartFile("uploads[0].file", "statement.csv", "text/csv",
                "01/08/2026;-10;Carte;;Achat;;;\n".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanSeeAndImportStatement() throws Exception {
        mvc.perform(get("/admin/data")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Relevés bancaires mensuels")));
        var response = mvc.perform(multipart("/admin/data/statements/import").file(csv())
                .param("uploads[0].year", "2026").param("uploads[0].month", "8").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/data"));
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).size());
        mvc.perform(get("/admin/data").flashAttrs(response.andReturn().getFlashMap()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("1 opérations importées.")));
        mvc.perform(get("/admin/data").param("year", "2026").param("month", "8"))
                .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Achat")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Résultat de l'import"))));
    }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminSeesSeparateResultsForTwoUploadedMonths() throws Exception {
        MockMultipartFile september = new MockMultipartFile("uploads[1].file", "september.csv", "text/csv",
            "01/09/2026;-20;Carte;;Septembre;;;\n".getBytes(StandardCharsets.UTF_8));
        var response = mvc.perform(multipart("/admin/data/statements/import").file(csv()).file(september)
                .param("uploads[0].year", "2026").param("uploads[0].month", "8")
                .param("uploads[1].year", "2026").param("uploads[1].month", "9").with(csrf()))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/data").flashAttrs(response.andReturn().getFlashMap()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Résultat de l'import")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("9/2026")));
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 9).size());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void oversizedRequestHasGeneralError() throws Exception {
        assertEquals("22MB", environment.getProperty("spring.servlet.multipart.max-request-size"));
        assertEquals("redirect:/admin/data?uploadTooLarge", uploadExceptionHandler.uploadTooLarge());
        mvc.perform(get("/admin/data").param("uploadTooLarge", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Lot trop volumineux")));
        }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCannotReadOrImportStatement() throws Exception {
        mvc.perform(get("/admin/data")).andExpect(status().isForbidden());
        mvc.perform(multipart("/admin/data/statements/import").file(csv())
                        .param("uploads[0].year", "2026").param("uploads[0].month", "8").with(csrf()))
                .andExpect(status().isForbidden());
        assertEquals(0, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).size());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void importRequiresCsrf() throws Exception {
        mvc.perform(multipart("/admin/data/statements/import").file(csv())
                        .param("uploads[0].year", "2026").param("uploads[0].month", "8"))
                .andExpect(status().isForbidden());
    }

    private void storeRow(int month, long line, StatementRow.Kind kind) {
        StatementRow row = new StatementRow();
        row.setYear(2026);
        row.setMonth(month);
        row.setLineNumber(line);
        row.setDate(LocalDate.of(2026, month, 1));
        row.setAmount(new BigDecimal("100"));
        row.setKind(kind);
        repository.saveAndFlush(row);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDeletesOperationsAndHistoricalBalancesOnlyForSelectedMonth() throws Exception {
        storeRow(8, 1, StatementRow.Kind.BALANCE);
        storeRow(8, 2, StatementRow.Kind.OPERATION);
        storeRow(9, 1, StatementRow.Kind.OPERATION);

        mvc.perform(post("/admin/data/statements/2026/8/delete").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/data"));
        assertEquals(0, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).size());
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 9).size());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRejectsInvalidPeriodAndMissingCsrf() throws Exception {
        storeRow(8, 1, StatementRow.Kind.BALANCE);
        mvc.perform(post("/admin/data/statements/2026/8/delete"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/data/statements/2026/13/delete").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).size());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCannotDeleteMonth() throws Exception {
        storeRow(8, 1, StatementRow.Kind.BALANCE);
        mvc.perform(post("/admin/data/statements/2026/8/delete").with(csrf()))
                .andExpect(status().isForbidden());
        assertEquals(1, repository.findByYearAndMonthOrderByLineNumberAsc(2026, 8).size());
    }
}