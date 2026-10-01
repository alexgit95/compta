package com.example.demo.controller;

import com.example.demo.model.RecurringExpense;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.service.FixedChargeDetectionService;
import com.example.demo.service.BudgetReconciliationService;
import com.example.demo.service.SavedForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/previsionnel")
@PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
@RequiredArgsConstructor
public class ForecastController {
    private final FixedChargeDetectionService detectionService;
    private final BudgetReconciliationService reconciliationService;
    private final SavedForecastService savedForecastService;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("activePage", "previsionnel");
        model.addAttribute("analysisRun", false);
        model.addAttribute("tolerancePercent", FixedChargeDetectionService.DEFAULT_TOLERANCE_PERCENT);
        savedForecastService.current().ifPresent(saved -> {
            model.addAttribute("tolerancePercent", saved.tolerancePercent());
            if (saved.stale()) {
                model.addAttribute("analysisStale", true);
            } else {
                populateAnalysis(model, saved.analysis(), saved.tolerancePercent());
            }
        });
        return "previsionnel";
    }

    @PostMapping("/analyse")
    public String analyze(@RequestParam(required = false) String tolerancePercent, Model model) {
        model.addAttribute("activePage", "previsionnel");
        model.addAttribute("analysisRun", false);
        model.addAttribute("tolerancePercent", tolerancePercent);
        BigDecimal percent;
        try {
            percent = new BigDecimal(tolerancePercent == null ? "" : tolerancePercent.trim().replace(',', '.'));
            detectionService.toleranceFraction(percent);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("toleranceError", "La tolérance doit être comprise entre 0 et 10 % par pas de 0,5.");
            return "previsionnel";
        }
        model.addAttribute("tolerancePercent", percent);
        model.addAttribute("analysisRun", true);
        detectionService.analyze(percent).ifPresent(analysis -> {
            savedForecastService.save(analysis, percent);
            populateAnalysis(model, analysis, percent);
        });
        return "previsionnel";
    }

    private void populateAnalysis(Model model, FixedChargeDetectionService.Analysis analysis, BigDecimal percent) {
        model.addAttribute("analysis", analysis);
        var matches = reconciliationService.reconcile(analysis, percent);
        model.addAttribute("matches", matches);
        model.addAttribute("unconfirmedExpenses", reconciliationService.unconfirmed(matches));
        model.addAttribute("monthlyTotal", reconciliationService.monthlyTotal(analysis));
    }

    @GetMapping("/candidates/{index}/new")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String newExpense(@PathVariable int index, @RequestParam String from,
                             @RequestParam int candidateKey, @RequestParam String tolerancePercent,
                             Model model, RedirectAttributes ra) {
        try {
            BigDecimal percent = parseTolerance(tolerancePercent);
            var selection = candidateFor(index, candidateKey, from, percent);
            var candidate = selection.candidate();
                var status = reconciliationService.reconcile(selection.analysis(), percent).get(index).status();
                if (status == BudgetReconciliationService.Status.RETROUVEE) {
                throw new IllegalArgumentException("Cette charge est déjà couverte dans Budget.");
            }
            RecurringExpense expense = new RecurringExpense();
            expense.setLabel(candidate.label().replace(" (libellé variable)", ""));
            expense.setAmount(candidate.amount());
            expense.setDayOfMonth(reconciliationService.dayOfMonth(candidate));
            model.addAttribute("expense", expense);
                    model.addAttribute("forecastPartial", status == BudgetReconciliationService.Status.A_VERIFIER);
            prepareForm(model, index, candidateKey, from, tolerancePercent);
            return "expense-form";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/previsionnel";
        }
    }

    @PostMapping("/candidates/save")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String saveExpense(@ModelAttribute("expense") RecurringExpense expense,
                              @RequestParam int candidateIndex, @RequestParam int candidateKey,
                              @RequestParam String from, @RequestParam String tolerancePercent,
                              Model model, RedirectAttributes ra) {
        try {
            BigDecimal percent = parseTolerance(tolerancePercent);
            var selection = candidateFor(candidateIndex, candidateKey, from, percent);
            reconciliationService.saveDetectedExpense(selection.candidate(), percent, expense);
            ra.addFlashAttribute("success", "Dépense récurrente enregistrée.");
            return "redirect:/budget";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            prepareForm(model, candidateIndex, candidateKey, from, tolerancePercent);
            return "expense-form";
        }
    }

    private void prepareForm(Model model, int index, int candidateKey, String from, String tolerancePercent) {
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("forecastSource", true);
        model.addAttribute("candidateIndex", index);
        model.addAttribute("candidateKey", candidateKey);
        model.addAttribute("from", from);
        model.addAttribute("tolerancePercent", tolerancePercent);
    }

    private record Selection(FixedChargeDetectionService.Analysis analysis,
                             FixedChargeDetectionService.Candidate candidate) { }

    private Selection candidateFor(int index, int key, String from, BigDecimal percent) {
        YearMonth expectedMonth;
        try {
            expectedMonth = YearMonth.parse(from);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Période d'analyse invalide.", ex);
        }
        var analysis = savedForecastService.current()
            .filter(saved -> !saved.stale() && saved.tolerancePercent().compareTo(percent) == 0)
            .map(SavedForecastService.SavedResult::analysis)
            .filter(result -> result.from().equals(expectedMonth))
                .orElseThrow(() -> new IllegalArgumentException("L'analyse a changé. Relancez-la avant d'ajouter une dépense."));
        if (index < 0 || index >= analysis.candidates().size()) {
            throw new IllegalArgumentException("Charge détectée introuvable.");
        }
        var candidate = analysis.candidates().get(index);
        if (candidate.cadence() != FixedChargeDetectionService.Cadence.MENSUELLE || candidate.hashCode() != key) {
            throw new IllegalArgumentException("La charge a changé. Relancez l'analyse.");
        }
        return new Selection(analysis, candidate);
    }

    private BigDecimal parseTolerance(String value) {
        BigDecimal percent = new BigDecimal(value.trim().replace(',', '.'));
        detectionService.toleranceFraction(percent);
        return percent;
    }
}