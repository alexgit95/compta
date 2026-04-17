package com.example.demo.controller;

import com.example.demo.model.Goal;
import com.example.demo.model.GoalType;
import com.example.demo.model.SavingsAccount;
import com.example.demo.service.GoalService;
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

@Controller
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final SavingsService savingsService;

    @GetMapping
    public String goals(@RequestParam(defaultValue = "12") int trendMonths, Model model) {
        List<SavingsAccount> accounts = savingsService.findAllAccounts();
        List<Goal> goals = goalService.findAllGoals();

        // Current estimated balance per account
        Map<Long, BigDecimal> currentValues = new LinkedHashMap<>();
        for (SavingsAccount account : accounts) {
            currentValues.put(account.getId(), savingsService.projectBalance(account, LocalDate.now()));
        }
        // Ensure every goal's account is represented (ZERO fallback for orphaned refs)
        for (Goal goal : goals) {
            Long aid = goal.getSavingsAccount().getId();
            currentValues.putIfAbsent(aid, BigDecimal.ZERO);
        }

        // Alerts: TARGET_BALANCE goals already reached but monthly deposit still active
        List<Goal> alerts = new ArrayList<>();
        for (Goal goal : goals) {
            if (goal.getType() == GoalType.TARGET_BALANCE) {
                BigDecimal current = currentValues.getOrDefault(goal.getSavingsAccount().getId(), BigDecimal.ZERO);
                if (current.compareTo(goal.getTargetAmount()) >= 0
                        && goal.getSavingsAccount().getMonthlyDeposit().compareTo(BigDecimal.ZERO) > 0) {
                    alerts.add(goal);
                }
            }
        }

        // Estimated reach dates (trend + projection) for balance goals
        Map<Long, Map<String, LocalDate>> reachDates = new LinkedHashMap<>();
        for (Goal goal : goals) {
            if (goal.getType() == GoalType.TARGET_BALANCE) {
                reachDates.put(goal.getId(), goalService.estimatedReachDates(goal, trendMonths));
            }
        }

        // Monthly contribution chart data (JS serialization via Thymeleaf inline)
        List<Map<String, Object>> monthlyContribData = new ArrayList<>();
        for (Goal goal : goals) {
            if (goal.getType() == GoalType.MONTHLY_CONTRIBUTION) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("label", goal.getSavingsAccount().getLabel());
                d.put("current", goal.getSavingsAccount().getMonthlyDeposit());
                d.put("target", goal.getTargetAmount());
                monthlyContribData.add(d);
            }
        }

        // Balance goals summary chart data
        List<Map<String, Object>> balanceGoalData = new ArrayList<>();
        for (Goal goal : goals) {
            if (goal.getType() == GoalType.TARGET_BALANCE) {
                BigDecimal current = currentValues.getOrDefault(goal.getSavingsAccount().getId(), BigDecimal.ZERO);
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("goalId", goal.getId());
                d.put("label", goal.getSavingsAccount().getLabel());
                d.put("current", current);
                d.put("target", goal.getTargetAmount());
                Map<String, LocalDate> rdMap = reachDates.get(goal.getId());
                LocalDate rd = rdMap != null ? rdMap.get("trend") : null;
                d.put("reachDate", rd != null ? rd.toString().substring(0, 7) : null);
                balanceGoalData.add(d);
            }
        }

        // Trend chart data per balance goal (goal.id → monthly data points)
        Map<Long, List<Map<String, Object>>> trendChartData = new LinkedHashMap<>();
        for (Goal goal : goals) {
            if (goal.getType() == GoalType.TARGET_BALANCE) {
                trendChartData.put(goal.getId(), goalService.getBalanceGoalChartData(goal, trendMonths));
            }
        }

        model.addAttribute("accounts", accounts);
        model.addAttribute("goals", goals);
        model.addAttribute("currentValues", currentValues);
        model.addAttribute("alerts", alerts);
        model.addAttribute("reachDates", reachDates);
        model.addAttribute("trendMonths", trendMonths);
        model.addAttribute("monthlyContribData", monthlyContribData);
        model.addAttribute("balanceGoalData", balanceGoalData);
        model.addAttribute("trendChartData", trendChartData);
        model.addAttribute("activePage", "goals");
        return "goals";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String saveGoal(@RequestParam Long savingsAccountId,
                           @RequestParam GoalType type,
                           @RequestParam BigDecimal targetAmount,
                           @RequestParam(required = false) Long goalId,
                           @RequestParam(defaultValue = "36") int trendMonths,
                           RedirectAttributes ra) {
        SavingsAccount account = savingsService.findAllAccounts().stream()
                .filter(a -> a.getId().equals(savingsAccountId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable : " + savingsAccountId));

        Goal goal;
        if (goalId != null) {
            goal = goalService.findAllGoals().stream()
                    .filter(g -> g.getId().equals(goalId))
                    .findFirst()
                    .orElse(new Goal());
        } else {
            goal = new Goal();
        }
        goal.setSavingsAccount(account);
        goal.setType(type);
        goal.setTargetAmount(targetAmount);
        goalService.saveGoal(goal);
        ra.addFlashAttribute("success", "Objectif enregistré.");
        return "redirect:/goals?trendMonths=" + trendMonths;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public String deleteGoal(@PathVariable Long id,
                             @RequestParam(defaultValue = "36") int trendMonths,
                             RedirectAttributes ra) {
        goalService.deleteGoal(id);
        ra.addFlashAttribute("success", "Objectif supprimé.");
        return "redirect:/goals?trendMonths=" + trendMonths;
    }
}
