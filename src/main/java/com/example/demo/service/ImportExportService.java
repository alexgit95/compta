package com.example.demo.service;

import com.example.demo.dto.ExportDto;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportExportService {

    private final CategoryRepository categoryRepository;
    private final RecurringExpenseRepository expenseRepository;
    private final SavingsAccountTypeRepository savingsAccountTypeRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsEntryRepository savingsEntryRepository;
    private final GoalRepository goalRepository;
    private final CreditRepository creditRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public ExportDto export() {
        ExportDto dto = new ExportDto();
        dto.setCategories(categoryRepository.findAll());
        dto.setRecurringExpenses(expenseRepository.findAll());
        dto.setSavingsAccountTypes(savingsAccountTypeRepository.findAll());
        dto.setSavingsAccounts(savingsAccountRepository.findAll());
        dto.setSavingsEntries(savingsEntryRepository.findAll());
        dto.setGoals(goalRepository.findAll());
        dto.setCredits(creditRepository.findAll());
        dto.setUsers(userRepository.findAll());
        return dto;
    }

    @Transactional
    public void importData(ExportDto dto) {
        // Clear all data first (order matters for FK constraints)
        // Goals reference SavingsAccount, so delete goals before savings entries/accounts
        goalRepository.deleteAllInBatch();
        savingsEntryRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
        savingsAccountRepository.deleteAllInBatch();
        savingsAccountTypeRepository.deleteAllInBatch();
        creditRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        // Flush deletes to DB before inserting to avoid UNIQUE constraint violations
        entityManager.flush();
        entityManager.clear();

        // Re-import: nullify IDs to let JPA generate new ones
        if (dto.getCategories() != null) {
            for (Category c : dto.getCategories()) {
                c.setId(null);
            }
            List<Category> savedCategories = categoryRepository.saveAll(dto.getCategories());

            if (dto.getRecurringExpenses() != null) {
                for (RecurringExpense e : dto.getRecurringExpenses()) {
                    e.setId(null);
                    // Re-link category by name
                    if (e.getCategory() != null) {
                        savedCategories.stream()
                                .filter(c -> c.getName().equals(e.getCategory().getName()))
                                .findFirst()
                                .ifPresent(e::setCategory);
                    }
                }
                expenseRepository.saveAll(dto.getRecurringExpenses());
            }
        }

        // Import account types
        List<SavingsAccountType> savedTypes = List.of();
        if (dto.getSavingsAccountTypes() != null) {
            for (SavingsAccountType t : dto.getSavingsAccountTypes()) {
                t.setId(null);
            }
            savedTypes = savingsAccountTypeRepository.saveAll(dto.getSavingsAccountTypes());
        }

        if (dto.getSavingsAccounts() != null) {
            for (SavingsAccount a : dto.getSavingsAccounts()) {
                a.setId(null);
                // Re-link account type by name
                if (a.getAccountType() != null) {
                    final String typeName = a.getAccountType().getName();
                    savedTypes.stream()
                            .filter(t -> t.getName().equals(typeName))
                            .findFirst()
                            .ifPresentOrElse(a::setAccountType, () -> a.setAccountType(null));
                }
            }
            List<SavingsAccount> savedAccounts = savingsAccountRepository.saveAll(dto.getSavingsAccounts());

            if (dto.getSavingsEntries() != null) {
                for (SavingsEntry e : dto.getSavingsEntries()) {
                    e.setId(null);
                    if (e.getSavingsAccount() != null) {
                        savedAccounts.stream()
                                .filter(a -> a.getLabel().equals(e.getSavingsAccount().getLabel()))
                                .findFirst()
                                .ifPresent(e::setSavingsAccount);
                    }
                }
                savingsEntryRepository.saveAll(dto.getSavingsEntries());
            }

            if (dto.getGoals() != null) {
                for (Goal g : dto.getGoals()) {
                    g.setId(null);
                    if (g.getSavingsAccount() != null) {
                        savedAccounts.stream()
                                .filter(a -> a.getLabel().equals(g.getSavingsAccount().getLabel()))
                                .findFirst()
                                .ifPresent(g::setSavingsAccount);
                    }
                }
                goalRepository.saveAll(dto.getGoals());
            }
        }

        if (dto.getUsers() != null) {
            for (User u : dto.getUsers()) {
                u.setId(null);
            }
            userRepository.saveAll(dto.getUsers());
        }

        if (dto.getCredits() != null) {
            for (Credit c : dto.getCredits()) {
                c.setId(null);
            }
            creditRepository.saveAll(dto.getCredits());
        }
    }
}
