package com.example.demo.controller;

import com.example.demo.model.Category;
import com.example.demo.model.RecurringExpense;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.service.AppSettingService;
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
import java.util.*;

@Controller
@RequestMapping("/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryRepository categoryRepository;
    private final AppSettingService appSettingService;

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

        // Convert LocalDate keys to ISO strings for safe JS inline serialization
        Map<String, BigDecimal> projectionStr = new LinkedHashMap<>();
        projection.forEach((k, v) -> projectionStr.put(k.toString(), v));

        model.addAttribute("projection", projectionStr);
        model.addAttribute("currentBalance", balance);
        model.addAttribute("endOfMonthBalance", endOfMonthBalance);
        model.addAttribute("totalMonthExpenses", totalMonthExpenses);
        model.addAttribute("endOfMonth", today.withDayOfMonth(today.lengthOfMonth()));
        model.addAttribute("today", today);
        model.addAttribute("expenses", budgetService.findAllExpenses());

        // --- Sankey data ---
        BigDecimal salary = appSettingService.getNumeric(AppSettingService.KEY_BUDGET_SALARY, BigDecimal.ZERO);
        BigDecimal courses = appSettingService.getNumeric(AppSettingService.KEY_BUDGET_COURSES, BigDecimal.ZERO);

        // Build Sankey links: [from, to, value]
        // Level 1: Salaires → Category
        // Level 2: Category → RecurringExpense
        // Special nodes (courses, remaining budget) are grouped under "Divers"
        List<List<Object>> sankeyLinks = new ArrayList<>();
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        final String otherCategory = "📦 Divers";

        List<RecurringExpense> allExpenses = budgetService.findAllExpenses();
        for (RecurringExpense exp : allExpenses) {
            String catName = exp.getCategory() != null
                    ? (exp.getCategory().getIcon() != null ? exp.getCategory().getIcon() + " " : "") + exp.getCategory().getName()
                    : "Sans catégorie";
            categoryTotals.merge(catName, exp.getAmount(), BigDecimal::add);
        }

        // Salary → each category
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            sankeyLinks.add(Arrays.asList("💰 Salaires", entry.getKey(), entry.getValue()));
        }
        // Salary → Divers → Courses (special)
        if (courses.compareTo(BigDecimal.ZERO) > 0) {
            sankeyLinks.add(Arrays.asList("💰 Salaires", otherCategory, courses));
            sankeyLinks.add(Arrays.asList(otherCategory, "🛒 Courses", courses));
        }

        // Category → each expense
        for (RecurringExpense exp : allExpenses) {
            String catName = exp.getCategory() != null
                    ? (exp.getCategory().getIcon() != null ? exp.getCategory().getIcon() + " " : "") + exp.getCategory().getName()
                    : "Sans catégorie";
            sankeyLinks.add(Arrays.asList(catName, exp.getLabel(), exp.getAmount()));
        }

        model.addAttribute("sankeyLinks", sankeyLinks);
        model.addAttribute("sankeySalary", salary);
        model.addAttribute("sankeyCourses", courses);
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
