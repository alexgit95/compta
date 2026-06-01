package com.example.demo.controller;

import com.example.demo.model.SavingsAccount;
import com.example.demo.model.SavingsAccountType;
import com.example.demo.model.SavingsEntry;
import com.example.demo.service.SavingsAccountTypeService;
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
    private final SavingsAccountTypeService accountTypeService;

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
        // Accounts with no entry or last entry older than 30 days
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<SavingsAccount> staleAccounts = accounts.stream()
                .filter(a -> {
                    com.example.demo.model.SavingsEntry last = lastEntries.get(a.getId());
                    return last == null || last.getEntryDate().isBefore(thirtyDaysAgo);
                })
                .toList();

        model.addAttribute("accounts", accounts);
        model.addAttribute("chartData", chartData);
        model.addAttribute("currentValues", currentValues);
        model.addAttribute("lastEntries", lastEntries);
        model.addAttribute("staleAccounts", staleAccounts);
        model.addAttribute("chartMinDate", globalMin.withDayOfMonth(1).toString());
        model.addAttribute("chartMaxDate", globalMax.withDayOfMonth(1).toString());
        // Default view range: 6 months before today to 6 months after today
        model.addAttribute("chartDefaultFrom", LocalDate.now().minusMonths(6).withDayOfMonth(1).toString());
        model.addAttribute("chartDefaultTo", LocalDate.now().plusMonths(6).withDayOfMonth(1).toString());
        model.addAttribute("varDefaultFrom", LocalDate.now().minusYears(1).withDayOfMonth(1).toString());
        model.addAttribute("varDefaultTo", LocalDate.now().withDayOfMonth(1).toString());
        model.addAttribute("newAccount", new SavingsAccount());

        // Account types for dropdown and pie chart
        List<SavingsAccountType> accountTypes = accountTypeService.findAll();
        model.addAttribute("accountTypes", accountTypes);

        // Pie chart: distribution by type
        Map<String, BigDecimal> distributionByType = new LinkedHashMap<>();
        Map<String, String> iconByType = new LinkedHashMap<>();
        Map<String, String> pctByType = new LinkedHashMap<>();
        BigDecimal totalSavings = BigDecimal.ZERO;
        for (SavingsAccount account : accounts) {
            BigDecimal value = currentValues.getOrDefault(account.getId(), BigDecimal.ZERO);
            String typeName = account.getAccountType() != null ? account.getAccountType().getName() : "Non classé";
            String typeIcon = account.getAccountType() != null ? account.getAccountType().getIcon() : "❓";
            distributionByType.merge(typeName, value, BigDecimal::add);
            iconByType.putIfAbsent(typeName, typeIcon);
            totalSavings = totalSavings.add(value);
        }
        // Compute percentage strings server-side to avoid complex SpEL in template
        for (Map.Entry<String, BigDecimal> entry : distributionByType.entrySet()) {
            if (totalSavings.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = entry.getValue().multiply(BigDecimal.valueOf(100))
                        .divide(totalSavings, 1, java.math.RoundingMode.HALF_UP);
                pctByType.put(entry.getKey(), pct.toPlainString() + " %");
            } else {
                pctByType.put(entry.getKey(), "0 %");
            }
        }
        model.addAttribute("distributionByType", distributionByType);
        model.addAttribute("iconByType", iconByType);
        model.addAttribute("pctByType", pctByType);
        model.addAttribute("totalSavings", totalSavings);

        // Scoring / advice: recommended distribution vs actual
        List<Map<String, Object>> allocationAdvice = new ArrayList<>();
        for (SavingsAccountType type : accountTypes) {
            Map<String, Object> advice = new LinkedHashMap<>();
            advice.put("type", type.getName());
            advice.put("icon", type.getIcon());
            advice.put("recommendedPct", type.getRecommendedPercentage());
            BigDecimal actual = distributionByType.getOrDefault(type.getName(), BigDecimal.ZERO);
            BigDecimal actualPct = totalSavings.compareTo(BigDecimal.ZERO) > 0
                    ? actual.multiply(BigDecimal.valueOf(100)).divide(totalSavings, 1, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            advice.put("actualPct", actualPct);
            advice.put("actualAmount", actual);
            BigDecimal recommendedAmount = totalSavings.multiply(BigDecimal.valueOf(type.getRecommendedPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            advice.put("recommendedAmount", recommendedAmount);
            BigDecimal diff = actual.subtract(recommendedAmount);
            advice.put("diff", diff);
            allocationAdvice.add(advice);
        }
        model.addAttribute("allocationAdvice", allocationAdvice);

        return "savings";
    }

    @PostMapping("/accounts/save")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String saveAccount(@ModelAttribute SavingsAccount account,
                              @RequestParam(required = false) Long accountTypeId,
                              RedirectAttributes ra) {
        if (accountTypeId != null) {
            account.setAccountType(accountTypeService.findById(accountTypeId));
        } else {
            account.setAccountType(null);
        }
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
        model.addAttribute("accountTypes", accountTypeService.findAll());
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
