package com.example.demo.controller;

import com.example.demo.model.StatementRow;
import com.example.demo.model.RecurringExpense;
import com.example.demo.repository.RecurringExpenseRepository;
import com.example.demo.repository.StatementRowRepository;
import com.example.demo.repository.SavedForecastAnalysisRepository;
import com.example.demo.service.FixedChargeDetectionService;
import com.example.demo.service.StatementImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:forecast-web;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ForecastControllerTest {
    @TestConfiguration
    static class FixedClockConfig {
        @Bean @Primary
        Clock testForecastClock() {
            return Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneId.of("Europe/Paris"));
        }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private StatementRowRepository statementRepository;
    @Autowired private SavedForecastAnalysisRepository savedRepository;
    @Autowired private StatementImportService importService;
    @Autowired private RecurringExpenseRepository expenseRepository;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        savedRepository.findById(1L).ifPresent(savedRepository::delete);
        savedRepository.flush();
        statementRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    private FixedChargeDetectionService.Candidate monthlyCandidate() throws Exception {
        for (int month : new int[]{7, 8, 9}) store(month, "-50", "CB Cotisation");
        var response = mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "5").with(csrf()))
                .andExpect(status().isOk()).andReturn();
        var analysis = (FixedChargeDetectionService.Analysis) response.getModelAndView().getModel().get("analysis");
        return analysis.candidates().getFirst();
    }

    private void store(int month, String amount, String label) {
        StatementRow row = new StatementRow();
        row.setYear(2026);
        row.setMonth(month);
        row.setLineNumber(1);
        row.setDate(LocalDate.of(2026, month, 3));
        row.setAmount(new BigDecimal(amount));
        row.setDebitLabel(label);
        row.setKind(StatementRow.Kind.OPERATION);
        statementRepository.saveAndFlush(row);
    }

    private void storeTransfer(int month, String label) {
        StatementRow row = new StatementRow();
        row.setYear(2026);
        row.setMonth(month);
        row.setLineNumber(1);
        row.setDate(LocalDate.of(2026, month, month == 8 ? 4 : 3));
        row.setAmount(new BigDecimal("-300"));
        row.setTransactionType("Virement");
        row.setDebitLabel(label);
        row.setKind(StatementRow.Kind.OPERATION);
        statementRepository.saveAndFlush(row);
    }

    private void storeWeekly(LocalDate date, long lineNumber) {
        StatementRow row = new StatementRow();
        row.setYear(date.getYear());
        row.setMonth(date.getMonthValue());
        row.setLineNumber(lineNumber);
        row.setDate(date);
        row.setAmount(new BigDecimal("-20"));
        row.setDebitLabel("CB Sport");
        row.setKind(StatementRow.Kind.OPERATION);
        statementRepository.saveAndFlush(row);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCanOpenAndRunButRequiresCsrf() throws Exception {
        mvc.perform(get("/previsionnel")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Analyser les charges fixes")));
        mvc.perform(post("/previsionnel/analyse"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "4").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Historique insuffisant")));
    }

    @Test
    @WithMockUser(roles = "API")
    void unrelatedRoleCannotAccess() throws Exception {
        mvc.perform(get("/previsionnel")).andExpect(status().isForbidden());
        mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "4").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void detectedChargesAreDisplayedWithoutChangingManualExpenses() throws Exception {
        for (int month : new int[]{7, 8, 9}) store(month, "-250", "VIR.PERMANENT Beau voyage LDDS");
        long manualExpensesBefore = expenseRepository.count();
        mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "4").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEAU VOYAGE LDDS")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2026-07 à 2026-09")));
        assertEquals(manualExpensesBefore, expenseRepository.count());
        assertEquals(3, statementRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reconciliationUsesSameToleranceAsDetection() throws Exception {
        for (int month : new int[]{7, 8, 9}) store(month, "-100", "CB Abonnement");
        RecurringExpense budget = new RecurringExpense();
        budget.setLabel("Autre libellé");
        budget.setAmount(new BigDecimal("104.50"));
        budget.setDayOfMonth(5);
        expenseRepository.saveAndFlush(budget);

        mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "4").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Non retrouvée")));
        mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "5").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Retrouvée")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Total mensuel des charges détectées")));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerSeesStatusButCannotSeeAddLink() throws Exception {
        for (int month : new int[]{7, 8, 9}) store(month, "-50", "CB Cotisation");
        mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "4").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Non retrouvée")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/previsionnel/candidates/0/new"))));
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void completedWindowWithoutRecurringSeriesShowsDifferentEmptyState() throws Exception {
        store(7, "-10", "CB Alpha");
        store(8, "-20", "CB Beta");
        store(9, "-30", "CB Gamma");
        mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "4").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Aucune charge fixe détectée")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsMalformedOrOutOfRangeToleranceWithoutRunningAnalysis() throws Exception {
        for (String invalid : new String[]{"", "0.1", "10.5", "-0.5", "abc"}) {
            mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", invalid).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("La tolérance doit être comprise")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Historique insuffisant"))));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void displaysAnObservedTransferLabelAsAnIllustration() throws Exception {
        storeTransfer(7, "VIR.PERMANENT Alexandre Boursor");
        storeTransfer(8, "VIR.PERMANENT A Boursor");
        storeTransfer(9, "VIR.PERMANENT AE Boursor");

        mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "4").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ALEXANDRE BOURSOR (libellé variable)")));
    }

            @Test
            @WithMockUser(roles = "ADMIN")
            void reviewedAdditionUsesSelectedToleranceAndRejectsSecondSave() throws Exception {
            var candidate = monthlyCandidate();
            var url = "/previsionnel/candidates/0/new";
            mvc.perform(get(url).param("candidateKey", String.valueOf(candidate.hashCode()))
                    .param("from", "2026-07").param("tolerancePercent", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/previsionnel/candidates/save")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("COTISATION")));
            assertEquals(0, expenseRepository.count());

            var request = post("/previsionnel/candidates/save").with(csrf())
                .param("candidateIndex", "0").param("candidateKey", String.valueOf(candidate.hashCode()))
                .param("from", "2026-07").param("tolerancePercent", "5")
                .param("label", "Cotisation personnalisée").param("amount", "50.00").param("dayOfMonth", "3");
            mvc.perform(request).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/budget"));
            assertEquals(1, expenseRepository.count());
            assertEquals("Cotisation personnalisée", expenseRepository.findAll().getFirst().getLabel());
            mvc.perform(request).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("existe déjà")));
            assertEquals(1, expenseRepository.count());
            }

            @Test
            @WithMockUser(roles = "VIEWER")
            void viewerCannotUseAdditionRoute() throws Exception {
            var candidate = monthlyCandidate();
            mvc.perform(get("/previsionnel/candidates/0/new")
                    .param("candidateKey", String.valueOf(candidate.hashCode()))
                    .param("from", "2026-07").param("tolerancePercent", "5"))
                .andExpect(status().isForbidden());
            mvc.perform(post("/previsionnel/candidates/save").with(csrf())
                    .param("candidateIndex", "0").param("candidateKey", String.valueOf(candidate.hashCode()))
                    .param("from", "2026-07").param("tolerancePercent", "5")
                    .param("label", "X").param("amount", "50").param("dayOfMonth", "3"))
                .andExpect(status().isForbidden());
            }

            @Test
            @WithMockUser(roles = "EDITOR")
            void staleOrMalformedCandidateCannotBeAdded() throws Exception {
            var candidate = monthlyCandidate();
            mvc.perform(get("/previsionnel/candidates/0/new")
                    .param("candidateKey", String.valueOf(candidate.hashCode()))
                    .param("from", "invalid").param("tolerancePercent", "5"))
                .andExpect(status().is3xxRedirection());
            statementRepository.deleteAllInBatch();
            mvc.perform(post("/previsionnel/candidates/save").with(csrf())
                    .param("candidateIndex", "0").param("candidateKey", String.valueOf(candidate.hashCode()))
                    .param("from", "2026-07").param("tolerancePercent", "5")
                    .param("label", "X").param("amount", "50").param("dayOfMonth", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Relancez")));
            assertEquals(0, expenseRepository.count());
            }

        @Test
        @WithMockUser(roles = "ADMIN")
        void addingRequiresCsrfEvenForAnEligibleCandidate() throws Exception {
            var candidate = monthlyCandidate();
            mvc.perform(post("/previsionnel/candidates/save")
                            .param("candidateIndex", "0").param("candidateKey", String.valueOf(candidate.hashCode()))
                            .param("from", "2026-07").param("tolerancePercent", "5")
                            .param("label", "Cotisation").param("amount", "50").param("dayOfMonth", "3"))
                    .andExpect(status().isForbidden());
            assertEquals(0, expenseRepository.count());
        }

            @Test
            @WithMockUser(roles = "ADMIN")
            void weeklyChargeIsVisibleButExcludedFromTotalAndAddAction() throws Exception {
            storeWeekly(LocalDate.of(2026, 7, 28), 1);
            storeWeekly(LocalDate.of(2026, 8, 4), 1);
            storeWeekly(LocalDate.of(2026, 8, 11), 2);
            store(9, "-10", "CB Autre");

            var response = mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "4").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Non comparable")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                    "/previsionnel/candidates/0/new"))))
                .andReturn();
            var analysis = (FixedChargeDetectionService.Analysis) response.getModelAndView().getModel().get("analysis");
            assertEquals(0, ((BigDecimal) response.getModelAndView().getModel().get("monthlyTotal"))
                .compareTo(BigDecimal.ZERO));
            mvc.perform(get("/previsionnel/candidates/0/new")
                    .param("candidateKey", String.valueOf(analysis.candidates().getFirst().hashCode()))
                    .param("from", "2026-07").param("tolerancePercent", "4"))
                .andExpect(status().is3xxRedirection());
            }

            @Test
            @WithMockUser(roles = "ADMIN")
            void reopensSavedAnalysisAndShowsStaleAfterSuccessfulReimport() throws Exception {
            for (int month : new int[]{7, 8, 9}) store(month, "-250", "VIR.PERMANENT Beau voyage LDDS");
            mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "5").with(csrf()))
                .andExpect(status().isOk());
            mvc.perform(get("/previsionnel"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEAU VOYAGE LDDS")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"5.0\"")));

            MockMultipartFile invalid = new MockMultipartFile("file", "bad.csv", "text/csv",
                "01/07/2026;-1;Carte;;Autre;;;".getBytes(StandardCharsets.UTF_8));
            assertThrows(IllegalArgumentException.class, () -> importService.importMonth(invalid, 2026, 8));
            mvc.perform(get("/previsionnel")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEAU VOYAGE LDDS")));

            MockMultipartFile replacement = new MockMultipartFile("file", "august.csv", "text/csv",
                "01/08/2026;-250;Virement;;VIR.PERMANENT Nouveau LDDS;;;"
                    .getBytes(StandardCharsets.UTF_8));
            importService.importMonth(replacement, 2026, 8);
            mvc.perform(get("/previsionnel"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Analyse à relancer")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("BEAU VOYAGE LDDS"))));
            }

            @Test
            @WithMockUser(roles = "ADMIN")
            void budgetListRefreshesWithoutRewritingSavedCandidates() throws Exception {
            for (int month : new int[]{7, 8, 9}) store(month, "-50", "CB Cotisation");
            RecurringExpense unrelated = new RecurringExpense();
            unrelated.setLabel("Loyer hors relevés");
            unrelated.setAmount(new BigDecimal("700"));
            unrelated.setDayOfMonth(10);
            expenseRepository.saveAndFlush(unrelated);
            mvc.perform(post("/previsionnel/analyse").param("tolerancePercent", "4").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Non identifiée dans les charges fixes")));
            var analyzedAt = savedRepository.findById(1L).orElseThrow().getAnalyzedAt();

            RecurringExpense confirmed = new RecurringExpense();
            confirmed.setLabel("Budget à libellé différent");
            confirmed.setAmount(new BigDecimal("50"));
            confirmed.setDayOfMonth(3);
            expenseRepository.saveAndFlush(confirmed);
            mvc.perform(get("/previsionnel")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Loyer hors relevés")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("Correspondance incertaine"))));
            assertEquals(analyzedAt, savedRepository.findById(1L).orElseThrow().getAnalyzedAt());

            expenseRepository.delete(unrelated);
            expenseRepository.flush();
            mvc.perform(get("/previsionnel")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Toutes les dépenses Budget")));
            assertEquals(analyzedAt, savedRepository.findById(1L).orElseThrow().getAnalyzedAt());
            }
}