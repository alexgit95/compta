package com.example.demo.dto;

import com.example.demo.model.*;
import lombok.Data;

import java.util.List;

@Data
public class ExportDto {
    private List<Category> categories;
    private List<RecurringExpense> recurringExpenses;
    private List<SavingsAccountType> savingsAccountTypes;
    private List<SavingsAccount> savingsAccounts;
    private List<SavingsEntry> savingsEntries;
    private List<Goal> goals;
    private List<Credit> credits;
    private List<Property> properties;
    private List<User> users;
    private List<AppSetting> appSettings;
    private List<ShoppingSettings> shoppingSettings;
}
