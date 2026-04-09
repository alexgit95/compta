package com.example.demo.service;

import com.example.demo.dto.ExportDto;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Transactional
class ImportExportServiceTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RecurringExpenseRepository expenseRepository;

    @Autowired
    private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private SavingsEntryRepository savingsEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private ImportExportService makeService() {
        return new ImportExportService(categoryRepository, expenseRepository, savingsAccountRepository,
                savingsEntryRepository, userRepository, entityManager);
    }

    @Test
    void exportReturnsAllEntities() {
        // ensure clean DB state for test
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        userRepository.deleteAll();

        Category cat = new Category();
        cat.setName("Housing");
        cat.setIcon("🏠");
        categoryRepository.save(cat);

        RecurringExpense exp = new RecurringExpense();
        exp.setLabel("Rent");
        exp.setAmount(BigDecimal.valueOf(1000));
        exp.setDayOfMonth(1);
        exp.setCategory(cat);
        expenseRepository.save(exp);

        SavingsAccount acc = new SavingsAccount();
        acc.setLabel("Rainy");
        acc.setMonthlyDeposit(BigDecimal.valueOf(50));
        savingsAccountRepository.save(acc);

        SavingsEntry entry = new SavingsEntry();
        entry.setSavingsAccount(acc);
        entry.setBalance(BigDecimal.valueOf(200));
        entry.setEntryDate(LocalDate.now());
        savingsEntryRepository.save(entry);

        User u = new User();
        u.setUsername("alice");
        u.setPassword("pw");
        u.setRole(Role.VIEWER);
        userRepository.save(u);

        ImportExportService svc = makeService();
        ExportDto dto = svc.export();

        assertNotNull(dto);
        assertEquals(1, dto.getCategories().size());
        assertEquals(1, dto.getRecurringExpenses().size());
        assertEquals(1, dto.getSavingsAccounts().size());
        assertEquals(1, dto.getSavingsEntries().size());
        assertEquals(1, dto.getUsers().size());
    }

    @Test
    void importDataClearsAndReimports() {
        // ensure clean DB state and seed DB with a leftover record
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        userRepository.deleteAll();

        Category old = new Category();
        old.setName("Old");
        categoryRepository.save(old);

        // prepare DTO to import
        Category c = new Category();
        c.setName("Food");

        RecurringExpense e = new RecurringExpense();
        e.setLabel("Groceries");
        e.setAmount(BigDecimal.valueOf(200));
        e.setDayOfMonth(5);
        e.setCategory(c);

        SavingsAccount a = new SavingsAccount();
        a.setLabel("Holiday");
        a.setMonthlyDeposit(BigDecimal.valueOf(100));

        SavingsEntry se = new SavingsEntry();
        se.setSavingsAccount(a);
        se.setBalance(BigDecimal.valueOf(300));
        se.setEntryDate(LocalDate.now());

        User u = new User();
        u.setUsername("bob");
        u.setPassword("pw");
        u.setRole(Role.VIEWER);

        ExportDto dto = new ExportDto();
        dto.setCategories(List.of(c));
        dto.setRecurringExpenses(List.of(e));
        dto.setSavingsAccounts(List.of(a));
        dto.setSavingsEntries(List.of(se));
        dto.setUsers(List.of(u));

        ImportExportService svc = makeService();
        svc.importData(dto);

        // after import, old should be gone and new counts match DTO
        assertEquals(1, categoryRepository.count());
        assertEquals(1, expenseRepository.count());
        assertEquals(1, savingsAccountRepository.count());
        assertEquals(1, savingsEntryRepository.count());
        assertEquals(1, userRepository.count());

        RecurringExpense saved = expenseRepository.findAll().get(0);
        assertNotNull(saved.getCategory());
        assertEquals("Food", saved.getCategory().getName());

        SavingsEntry savedEntry = savingsEntryRepository.findAll().get(0);
        assertNotNull(savedEntry.getSavingsAccount());
        assertEquals("Holiday", savedEntry.getSavingsAccount().getLabel());
    }
}
