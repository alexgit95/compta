package com.example.demo.service;

import com.example.demo.model.RecurringExpense;
import com.example.demo.repository.RecurringExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final RecurringExpenseRepository expenseRepository;

    public List<RecurringExpense> findAllExpenses() {
        return expenseRepository.findAllByOrderByDayOfMonth();
    }

    @Transactional
    public RecurringExpense save(RecurringExpense expense) {
        return expenseRepository.save(expense);
    }

    @Transactional
    public void delete(Long id) {
        expenseRepository.deleteById(id);
    }

    /**
     * Builds daily balance projection from today until end of month.
     * Returns a map of date -> projected balance.
     */
    public Map<LocalDate, BigDecimal> projectBalance(BigDecimal currentBalance) {
        LocalDate today = LocalDate.now();
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        List<RecurringExpense> remaining = expenseRepository
                .findByDayOfMonthGreaterThanEqualOrderByDayOfMonth(today.getDayOfMonth());

        Map<LocalDate, BigDecimal> projection = new LinkedHashMap<>();
        BigDecimal balance = currentBalance;
        projection.put(today, balance);

        for (LocalDate date = today.plusDays(1); !date.isAfter(endOfMonth); date = date.plusDays(1)) {
            final LocalDate d = date;
            BigDecimal expense = remaining.stream()
                    .filter(e -> e.getDayOfMonth() == d.getDayOfMonth())
                    .map(RecurringExpense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            balance = balance.subtract(expense);
            projection.put(date, balance);
        }
        return projection;
    }
}
