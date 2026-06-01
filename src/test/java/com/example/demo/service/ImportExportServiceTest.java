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
    private SavingsAccountTypeRepository savingsAccountTypeRepository;

    @Autowired
    private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private SavingsEntryRepository savingsEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private EntityManager entityManager;

    private ImportExportService makeService() {
        return new ImportExportService(categoryRepository, expenseRepository, savingsAccountTypeRepository,
                savingsAccountRepository, savingsEntryRepository, goalRepository, userRepository, entityManager);
    }

    @Test
    void exportReturnsAllEntities() {
        // ensure clean DB state for test
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        savingsAccountTypeRepository.deleteAll();
        userRepository.deleteAll();

        SavingsAccountType type = new SavingsAccountType("Livret", "💧", 30);
        savingsAccountTypeRepository.save(type);

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
        acc.setAccountType(type);
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
        assertEquals(1, dto.getSavingsAccountTypes().size());
        assertEquals(1, dto.getSavingsAccounts().size());
        assertEquals(1, dto.getSavingsEntries().size());
        assertEquals(1, dto.getUsers().size());
        assertEquals("Livret", dto.getSavingsAccountTypes().get(0).getName());
        assertEquals("Livret", dto.getSavingsAccounts().get(0).getAccountType().getName());
    }

    @Test
    void importDataClearsAndReimports() {
        // ensure clean DB state and seed DB with a leftover record
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        savingsAccountTypeRepository.deleteAll();
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

        SavingsAccountType type = new SavingsAccountType("PEA", "📈", 25);

        SavingsAccount a = new SavingsAccount();
        a.setLabel("Holiday");
        a.setMonthlyDeposit(BigDecimal.valueOf(100));
        a.setAccountType(type);

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
        dto.setSavingsAccountTypes(List.of(type));
        dto.setSavingsAccounts(List.of(a));
        dto.setSavingsEntries(List.of(se));
        dto.setUsers(List.of(u));

        ImportExportService svc = makeService();
        svc.importData(dto);

        // after import, old should be gone and new counts match DTO
        assertEquals(1, categoryRepository.count());
        assertEquals(1, expenseRepository.count());
        assertEquals(1, savingsAccountTypeRepository.count());
        assertEquals(1, savingsAccountRepository.count());
        assertEquals(1, savingsEntryRepository.count());
        assertEquals(1, userRepository.count());

        RecurringExpense saved = expenseRepository.findAll().get(0);
        assertNotNull(saved.getCategory());
        assertEquals("Food", saved.getCategory().getName());

        SavingsEntry savedEntry = savingsEntryRepository.findAll().get(0);
        assertNotNull(savedEntry.getSavingsAccount());
        assertEquals("Holiday", savedEntry.getSavingsAccount().getLabel());

        // Verify account type is preserved
        SavingsAccount savedAccount = savingsAccountRepository.findAll().get(0);
        assertNotNull(savedAccount.getAccountType());
        assertEquals("PEA", savedAccount.getAccountType().getName());
        assertEquals("📈", savedAccount.getAccountType().getIcon());
        assertEquals(25, savedAccount.getAccountType().getRecommendedPercentage());
    }

    @Test
    void importExportPreservesAccountTypesRoundTrip() {
        // Clean state
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        savingsAccountTypeRepository.deleteAll();
        userRepository.deleteAll();

        // Create types
        SavingsAccountType livret = new SavingsAccountType("Livret", "💧", 30);
        savingsAccountTypeRepository.save(livret);
        SavingsAccountType pea = new SavingsAccountType("PEA", "📈", 25);
        savingsAccountTypeRepository.save(pea);

        // Create accounts with types
        SavingsAccount acc1 = new SavingsAccount();
        acc1.setLabel("Livret A");
        acc1.setMonthlyDeposit(BigDecimal.valueOf(200));
        acc1.setAccountType(livret);
        savingsAccountRepository.save(acc1);

        SavingsAccount acc2 = new SavingsAccount();
        acc2.setLabel("PEA Bourse");
        acc2.setMonthlyDeposit(BigDecimal.valueOf(300));
        acc2.setAccountType(pea);
        savingsAccountRepository.save(acc2);

        SavingsAccount acc3 = new SavingsAccount();
        acc3.setLabel("Sans type");
        acc3.setMonthlyDeposit(BigDecimal.valueOf(50));
        acc3.setAccountType(null);
        savingsAccountRepository.save(acc3);

        ImportExportService svc = makeService();

        // Export
        ExportDto exported = svc.export();
        assertEquals(2, exported.getSavingsAccountTypes().size());
        assertEquals(3, exported.getSavingsAccounts().size());

        // Import (round trip)
        svc.importData(exported);

        // Verify types restored
        assertEquals(2, savingsAccountTypeRepository.count());
        assertEquals(3, savingsAccountRepository.count());

        List<SavingsAccount> accounts = savingsAccountRepository.findAll();

        SavingsAccount livretAccount = accounts.stream()
                .filter(a -> "Livret A".equals(a.getLabel())).findFirst().orElseThrow();
        assertNotNull(livretAccount.getAccountType());
        assertEquals("Livret", livretAccount.getAccountType().getName());
        assertEquals("💧", livretAccount.getAccountType().getIcon());
        assertEquals(30, livretAccount.getAccountType().getRecommendedPercentage());

        SavingsAccount peaAccount = accounts.stream()
                .filter(a -> "PEA Bourse".equals(a.getLabel())).findFirst().orElseThrow();
        assertNotNull(peaAccount.getAccountType());
        assertEquals("PEA", peaAccount.getAccountType().getName());

        SavingsAccount untyped = accounts.stream()
                .filter(a -> "Sans type".equals(a.getLabel())).findFirst().orElseThrow();
        assertNull(untyped.getAccountType());
    }
}
