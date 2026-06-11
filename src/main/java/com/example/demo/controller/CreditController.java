package com.example.demo.controller;

import com.example.demo.model.Credit;
import com.example.demo.service.CreditService;
import com.example.demo.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
@Controller
@RequestMapping("/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;
    private final PropertyService propertyService;

    @GetMapping
    public String list(Model model) {
        List<Credit> credits = creditService.findAll();

        // Build summary data for the table
        Map<Long, BigDecimal> repaymentPercentages = new LinkedHashMap<>();
        Map<Long, String> repaymentColors = new LinkedHashMap<>();
        Map<Long, String> remainingDurations = new LinkedHashMap<>();
        Map<Long, BigDecimal> currentRemainingAmounts = new LinkedHashMap<>();
        BigDecimal totalRemaining = BigDecimal.ZERO;
        BigDecimal totalMonthlyPayments = BigDecimal.ZERO;

        for (Credit credit : credits) {
            BigDecimal currentRemaining = creditService.getCurrentRemainingAmount(credit);
            currentRemainingAmounts.put(credit.getId(), currentRemaining);

            BigDecimal pct = creditService.getRepaymentPercentage(credit, currentRemaining);
            repaymentPercentages.put(credit.getId(), pct);
            String color;
            if (pct.compareTo(BigDecimal.valueOf(75)) >= 0) {
                color = "var(--success, #16a34a)";
            } else if (pct.compareTo(BigDecimal.valueOf(50)) >= 0) {
                color = "var(--warning, #f59e0b)";
            } else {
                color = "var(--primary)";
            }
            repaymentColors.put(credit.getId(), color);
            remainingDurations.put(credit.getId(), creditService.getRemainingDurationLabel(credit));
            totalRemaining = totalRemaining.add(currentRemaining);
            totalMonthlyPayments = totalMonthlyPayments.add(credit.getMonthlyPayment());
        }

        model.addAttribute("credits", credits);
        model.addAttribute("currentRemainingAmounts", currentRemainingAmounts);
        model.addAttribute("repaymentPercentages", repaymentPercentages);
        model.addAttribute("repaymentColors", repaymentColors);
        model.addAttribute("remainingDurations", remainingDurations);
        model.addAttribute("totalRemaining", totalRemaining);
        model.addAttribute("totalMonthlyPayments", totalMonthlyPayments);

        // Chart data: monthly remaining capital projection
        if (!credits.isEmpty()) {
            LocalDate today = LocalDate.now();
            LocalDate chartEnd = credits.stream()
                    .map(Credit::getEndDate)
                    .max(Comparator.naturalOrder())
                    .orElse(today.plusYears(1));

            DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
            List<String> chartLabels = new ArrayList<>();
            LocalDate cursor = today.withDayOfMonth(1);
            while (!cursor.isAfter(chartEnd.withDayOfMonth(1))) {
                chartLabels.add(cursor.format(labelFmt));
                cursor = cursor.plusMonths(1);
            }

            String[] palette = {"#3b82f6", "#f59e0b", "#10b981", "#ef4444", "#8b5cf6", "#ec4899", "#14b8a6"};
            List<Map<String, Object>> chartDatasets = new ArrayList<>();
            double[] totalRaw = new double[chartLabels.size()];
            int colorIdx = 0;

            for (Credit credit : credits) {
                List<Double> data = creditService.projectMonthlyRemaining(credit, today, chartEnd);
                Map<String, Object> ds = new LinkedHashMap<>();
                ds.put("label", credit.getLabel());
                ds.put("data", data);
                ds.put("color", palette[colorIdx % palette.length]);
                ds.put("isTotal", false);
                chartDatasets.add(ds);
                for (int i = 0; i < data.size() && i < totalRaw.length; i++) {
                    if (data.get(i) != null) totalRaw[i] += data.get(i);
                }
                colorIdx++;
            }

            // Build total list — use null once total stays at 0 (all credits repaid)
            List<Double> totalData = new ArrayList<>();
            boolean allDone = false;
            for (int i = 0; i < totalRaw.length; i++) {
                if (!allDone && totalRaw[i] == 0.0 && i > 0) allDone = true;
                totalData.add(allDone ? null : totalRaw[i]);
            }

            Map<String, Object> totalDs = new LinkedHashMap<>();
            totalDs.put("label", "Total");
            totalDs.put("data", totalData);
            totalDs.put("color", "#6b7280");
            totalDs.put("isTotal", true);
            chartDatasets.add(totalDs);

            model.addAttribute("chartLabels", chartLabels);
            model.addAttribute("chartDatasets", chartDatasets);
        }

        model.addAttribute("activePage", "credits");
        return "credits";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String save(@ModelAttribute Credit credit, RedirectAttributes ra) {
        creditService.save(credit);
        ra.addFlashAttribute("success", "Crédit enregistré.");
        return "redirect:/credits";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String editForm(@PathVariable Long id, Model model) {
        Credit credit = creditService.findById(id);
        model.addAttribute("credit", credit);
        model.addAttribute("properties", propertyService.findAll());
        model.addAttribute("activePage", "credits");
        return "credit-form";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        creditService.delete(id);
        ra.addFlashAttribute("success", "Crédit supprimé.");
        return "redirect:/credits";
    }
}
