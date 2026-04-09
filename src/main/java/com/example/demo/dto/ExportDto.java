package com.example.demo.dto;

import com.example.demo.model.*;
import lombok.Data;

import java.util.List;

@Data
public class ExportDto {
    private List<Category> categories;
    private List<RecurringExpense> recurringExpenses;
    private List<SavingsAccount> savingsAccounts;
    private List<SavingsEntry> savingsEntries;
    private List<User> users;
}
