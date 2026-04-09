package com.example.demo.repository;

import com.example.demo.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    List<RecurringExpense> findByDayOfMonthGreaterThanEqualOrderByDayOfMonth(int day);
    List<RecurringExpense> findAllByOrderByDayOfMonth();
}
