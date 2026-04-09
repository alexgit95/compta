package com.example.demo.controller;

import com.example.demo.model.SavingsAccount;
import com.example.demo.model.SavingsEntry;
import com.example.demo.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.Comparator;

@Controller
@RequestMapping("/savings")
@RequiredArgsConstructor
public class SavingsController {

    private final SavingsService savingsService;

    @GetMapping
    public String savings(Model model) {
        List<SavingsAccount> accounts = savingsService.findAllAccounts();

        // Compute global date range: min = first real entry, max = today + 2 years
        LocalDate globalMin = accounts.stream()
                .flatMap(a -> savingsService.findEntriesForAccount(a).stream())
                .map(com.example.demo.model.SavingsEntry::getEntryDate)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now().minusMonths(1));
        LocalDate globalMax = LocalDate.now().plusYears(2);

        Map<Long, List<Map<String, Object>>> chartData = new LinkedHashMap<>();
        Map<Long, BigDecimal> currentValues = new LinkedHashMap<>();
        Map<Long, com.example.demo.model.SavingsEntry> lastEntries = new LinkedHashMap<>();
        for (SavingsAccount account : accounts) {
            chartData.put(account.getId(), savingsService.getChartData(account, globalMin, globalMax));
            currentValues.put(account.getId(), savingsService.projectBalance(account, LocalDate.now()));
            List<com.example.demo.model.SavingsEntry> entries = savingsService.findEntriesForAccount(account);
            if (!entries.isEmpty()) {
                lastEntries.put(account.getId(), entries.get(entries.size() - 1));
            }
        }
        model.addAttribute("accounts", accounts);
        model.addAttribute("chartData", chartData);
        model.addAttribute("currentValues", currentValues);
        model.addAttribute("lastEntries", lastEntries);
        model.addAttribute("chartMinDate", globalMin.withDayOfMonth(1).toString());
        model.addAttribute("chartMaxDate", globalMax.withDayOfMonth(1).toString());
        // Default view range: 6 months before today to 6 months after today
        model.addAttribute("chartDefaultFrom", LocalDate.now().minusMonths(6).withDayOfMonth(1).toString());
        model.addAttribute("chartDefaultTo", LocalDate.now().plusMonths(6).withDayOfMonth(1).toString());
        model.addAttribute("varDefaultFrom", LocalDate.now().minusYears(1).withDayOfMonth(1).toString());
        model.addAttribute("varDefaultTo", LocalDate.now().withDayOfMonth(1).toString());
        model.addAttribute("newAccount", new SavingsAccount());
        return "savings";
    }

    @PostMapping("/accounts/save")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String saveAccount(@ModelAttribute SavingsAccount account, RedirectAttributes ra) {
        savingsService.saveAccount(account);
        ra.addFlashAttribute("success", "Compte épargne enregistré.");
        return "redirect:/savings";
    }

    @PostMapping("/accounts/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes ra) {
        savingsService.deleteAccount(id);
        ra.addFlashAttribute("success", "Compte supprimé.");
        return "redirect:/savings";
    }

    @PostMapping("/accounts/{id}/entry")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String addEntry(@PathVariable Long id,
                           @RequestParam BigDecimal balance,
                           @RequestParam String entryDate,
                           RedirectAttributes ra) {
        SavingsAccount account = savingsService.findAllAccounts().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        savingsService.addEntry(account, balance, LocalDate.parse(entryDate));
        ra.addFlashAttribute("success", "Valeur enregistrée.");
        return "redirect:/savings";
    }

    @GetMapping("/accounts/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String editForm(@PathVariable Long id, Model model) {
        SavingsAccount account = savingsService.findAllAccounts().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        model.addAttribute("account", account);
        model.addAttribute("entries", savingsService.findEntriesForAccount(account));
        return "savings-edit";
    }

    @PostMapping("/entries/{entryId}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String deleteEntry(@PathVariable Long entryId,
                              @RequestParam Long accountId,
                              RedirectAttributes ra) {
        savingsService.deleteEntry(entryId);
        ra.addFlashAttribute("success", "Valeur supprimée.");
        return "redirect:/savings/accounts/" + accountId + "/edit";
    }
}
