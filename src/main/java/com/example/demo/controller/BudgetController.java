package com.example.demo.controller;

import com.example.demo.model.Category;
import com.example.demo.model.RecurringExpense;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public String budget(@RequestParam(required = false) BigDecimal balance, Model model) {
        LocalDate today = LocalDate.now();
        if (balance == null) {
            // Default: sum of all expenses scheduled from today to end of month + 700€
            BigDecimal upcomingExpenses = budgetService.findAllExpenses().stream()
                    .filter(e -> e.getDayOfMonth() >= today.getDayOfMonth())
                    .map(e -> e.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            balance = upcomingExpenses.add(new BigDecimal("700"));
        }
        Map<LocalDate, BigDecimal> projection = budgetService.projectBalance(balance);

        // Compute end-of-month balance (last value in projection)
        BigDecimal endOfMonthBalance = projection.values().stream()
                .reduce(balance, (a, b) -> b);

        // Total expenses = difference between end and start
        BigDecimal totalMonthExpenses = balance.subtract(endOfMonthBalance).abs();

        model.addAttribute("projection", projection);
        model.addAttribute("currentBalance", balance);
        model.addAttribute("endOfMonthBalance", endOfMonthBalance);
        model.addAttribute("totalMonthExpenses", totalMonthExpenses);
        model.addAttribute("endOfMonth", today.withDayOfMonth(today.lengthOfMonth()));
        model.addAttribute("today", today);
        model.addAttribute("expenses", budgetService.findAllExpenses());
        return "budget";
    }

    @GetMapping("/expenses/new")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String newExpenseForm(Model model) {
        model.addAttribute("expense", new RecurringExpense());
        model.addAttribute("categories", categoryRepository.findAll());
        return "expense-form";
    }

    @GetMapping("/expenses/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String editExpenseForm(@PathVariable Long id, Model model) {
        RecurringExpense expense = budgetService.findAllExpenses().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
        model.addAttribute("expense", expense);
        model.addAttribute("categories", categoryRepository.findAll());
        return "expense-form";
    }

    @PostMapping("/expenses/save")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String saveExpense(@Valid @ModelAttribute("expense") RecurringExpense expense,
                              BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll());
            return "expense-form";
        }
        budgetService.save(expense);
        ra.addFlashAttribute("success", "Dépense enregistrée.");
        return "redirect:/budget";
    }

    @PostMapping("/expenses/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String deleteExpense(@PathVariable Long id, RedirectAttributes ra) {
        budgetService.delete(id);
        ra.addFlashAttribute("success", "Dépense supprimée.");
        return "redirect:/budget";
    }
}
