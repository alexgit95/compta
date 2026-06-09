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
    private CreditRepository creditRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private AppSettingRepository appSettingRepository;

    @Autowired
    private EntityManager entityManager;

    private ImportExportService makeService() {
        return new ImportExportService(categoryRepository, expenseRepository, savingsAccountTypeRepository,
                savingsAccountRepository, savingsEntryRepository, goalRepository, creditRepository, propertyRepository, userRepository, appSettingRepository, entityManager);
    }

    @Test
    void exportReturnsAllEntities() {
        // ensure clean DB state for test
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        savingsAccountTypeRepository.deleteAll();
        creditRepository.deleteAll();
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

        Credit credit = new Credit();
        credit.setLabel("Prêt immobilier");
        credit.setType("Immobilier");
        credit.setTotalAmount(BigDecimal.valueOf(200000));
        credit.setRate(BigDecimal.valueOf(1.5));
        credit.setStartDate(LocalDate.of(2020, 1, 1));
        credit.setEndDate(LocalDate.of(2040, 1, 1));
        credit.setMonthlyPayment(BigDecimal.valueOf(900));
        credit.setRemainingAmount(BigDecimal.valueOf(150000));
        credit.setRemainingAmountDate(LocalDate.now());
        creditRepository.save(credit);

        ImportExportService svc = makeService();
        ExportDto dto = svc.export();

        assertNotNull(dto);
        assertEquals(1, dto.getCategories().size());
        assertEquals(1, dto.getRecurringExpenses().size());
        assertEquals(1, dto.getSavingsAccountTypes().size());
        assertEquals(1, dto.getSavingsAccounts().size());
        assertEquals(1, dto.getSavingsEntries().size());
        assertEquals(1, dto.getCredits().size());
        assertEquals(1, dto.getUsers().size());
        assertEquals("Livret", dto.getSavingsAccountTypes().get(0).getName());
        assertEquals("Livret", dto.getSavingsAccounts().get(0).getAccountType().getName());
        assertEquals("Prêt immobilier", dto.getCredits().get(0).getLabel());
    }

    @Test
    void importDataClearsAndReimports() {
        // ensure clean DB state and seed DB with a leftover record
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        savingsAccountTypeRepository.deleteAll();
        creditRepository.deleteAll();
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

        Credit credit = new Credit();
        credit.setLabel("Prêt auto");
        credit.setType("Automobile");
        credit.setTotalAmount(BigDecimal.valueOf(15000));
        credit.setRate(BigDecimal.valueOf(3.5));
        credit.setStartDate(LocalDate.of(2023, 6, 1));
        credit.setEndDate(LocalDate.of(2028, 6, 1));
        credit.setMonthlyPayment(BigDecimal.valueOf(280));
        credit.setRemainingAmount(BigDecimal.valueOf(10000));
        credit.setRemainingAmountDate(LocalDate.now());

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
        dto.setCredits(List.of(credit));
        dto.setUsers(List.of(u));

        ImportExportService svc = makeService();
        svc.importData(dto);

        // after import, old should be gone and new counts match DTO
        assertEquals(1, categoryRepository.count());
        assertEquals(1, expenseRepository.count());
        assertEquals(1, savingsAccountTypeRepository.count());
        assertEquals(1, savingsAccountRepository.count());
        assertEquals(1, savingsEntryRepository.count());
        assertEquals(1, creditRepository.count());
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

        // Verify credit is preserved
        Credit savedCredit = creditRepository.findAll().get(0);
        assertEquals("Prêt auto", savedCredit.getLabel());
        assertEquals("Automobile", savedCredit.getType());
        assertEquals(0, BigDecimal.valueOf(15000).compareTo(savedCredit.getTotalAmount()));
        assertEquals(0, BigDecimal.valueOf(3.5).compareTo(savedCredit.getRate()));
    }

    @Test
    void importExportPreservesAccountTypesRoundTrip() {
        // Clean state
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        savingsAccountTypeRepository.deleteAll();
        creditRepository.deleteAll();
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

    @Test
    void importExportPreservesCreditsRoundTrip() {
        // Clean state
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        savingsAccountTypeRepository.deleteAll();
        creditRepository.deleteAll();
        userRepository.deleteAll();

        // Create credits
        Credit credit1 = new Credit();
        credit1.setLabel("Prêt immobilier");
        credit1.setType("Immobilier");
        credit1.setTotalAmount(BigDecimal.valueOf(200000));
        credit1.setRate(BigDecimal.valueOf(1.5));
        credit1.setStartDate(LocalDate.of(2020, 1, 1));
        credit1.setEndDate(LocalDate.of(2040, 1, 1));
        credit1.setMonthlyPayment(BigDecimal.valueOf(900));
        credit1.setRemainingAmount(BigDecimal.valueOf(150000));
        credit1.setRemainingAmountDate(LocalDate.of(2025, 6, 1));
        creditRepository.save(credit1);

        Credit credit2 = new Credit();
        credit2.setLabel("Prêt auto");
        credit2.setType("Automobile");
        credit2.setTotalAmount(BigDecimal.valueOf(20000));
        credit2.setRate(BigDecimal.valueOf(3.2));
        credit2.setStartDate(LocalDate.of(2022, 3, 1));
        credit2.setEndDate(LocalDate.of(2027, 3, 1));
        credit2.setMonthlyPayment(BigDecimal.valueOf(360));
        credit2.setRemainingAmount(BigDecimal.valueOf(8000));
        credit2.setRemainingAmountDate(LocalDate.of(2025, 6, 1));
        creditRepository.save(credit2);

        ImportExportService svc = makeService();

        // Export
        ExportDto exported = svc.export();
        assertEquals(2, exported.getCredits().size());

        // Import (round trip)
        svc.importData(exported);

        // Verify credits restored
        assertEquals(2, creditRepository.count());

        List<Credit> credits = creditRepository.findAll();

        Credit immobilier = credits.stream()
                .filter(c -> "Prêt immobilier".equals(c.getLabel())).findFirst().orElseThrow();
        assertEquals("Immobilier", immobilier.getType());
        assertEquals(0, BigDecimal.valueOf(200000).compareTo(immobilier.getTotalAmount()));
        assertEquals(0, BigDecimal.valueOf(1.5).compareTo(immobilier.getRate()));
        assertEquals(LocalDate.of(2020, 1, 1), immobilier.getStartDate());
        assertEquals(LocalDate.of(2040, 1, 1), immobilier.getEndDate());
        assertEquals(0, BigDecimal.valueOf(900).compareTo(immobilier.getMonthlyPayment()));
        assertEquals(0, BigDecimal.valueOf(150000).compareTo(immobilier.getRemainingAmount()));
        assertEquals(LocalDate.of(2025, 6, 1), immobilier.getRemainingAmountDate());

        Credit auto = credits.stream()
                .filter(c -> "Prêt auto".equals(c.getLabel())).findFirst().orElseThrow();
        assertEquals("Automobile", auto.getType());
        assertEquals(0, BigDecimal.valueOf(20000).compareTo(auto.getTotalAmount()));
        assertEquals(0, BigDecimal.valueOf(3.2).compareTo(auto.getRate()));
    }

    @Test
    void importExportPreservesPropertiesRoundTrip() {
        // Clean state
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        savingsAccountTypeRepository.deleteAll();
        creditRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();

        // Create properties
        Property prop1 = new Property();
        prop1.setLabel("Appartement Paris");
        prop1.setPurchaseValue(BigDecimal.valueOf(250000));
        prop1.setPurchaseDate(LocalDate.of(2018, 6, 15));
        prop1.setCurrentValue(BigDecimal.valueOf(310000));
        propertyRepository.save(prop1);

        Property prop2 = new Property();
        prop2.setLabel("Maison Bordeaux");
        prop2.setPurchaseValue(BigDecimal.valueOf(180000));
        prop2.setPurchaseDate(LocalDate.of(2021, 3, 1));
        prop2.setCurrentValue(BigDecimal.valueOf(210000));
        propertyRepository.save(prop2);

        ImportExportService svc = makeService();

        // Export
        ExportDto exported = svc.export();
        assertNotNull(exported.getProperties());
        assertEquals(2, exported.getProperties().size());

        // Import (round trip)
        svc.importData(exported);

        // Verify properties restored
        assertEquals(2, propertyRepository.count());

        List<Property> properties = propertyRepository.findAll();

        Property paris = properties.stream()
                .filter(p -> "Appartement Paris".equals(p.getLabel())).findFirst().orElseThrow();
        assertEquals(0, BigDecimal.valueOf(250000).compareTo(paris.getPurchaseValue()));
        assertEquals(LocalDate.of(2018, 6, 15), paris.getPurchaseDate());
        assertEquals(0, BigDecimal.valueOf(310000).compareTo(paris.getCurrentValue()));

        Property bordeaux = properties.stream()
                .filter(p -> "Maison Bordeaux".equals(p.getLabel())).findFirst().orElseThrow();
        assertEquals(0, BigDecimal.valueOf(180000).compareTo(bordeaux.getPurchaseValue()));
        assertEquals(LocalDate.of(2021, 3, 1), bordeaux.getPurchaseDate());
        assertEquals(0, BigDecimal.valueOf(210000).compareTo(bordeaux.getCurrentValue()));
    }

    @Test
    void importExportPreservesCreditPropertyLinkRoundTrip() {
        // Clean state
        categoryRepository.deleteAll();
        expenseRepository.deleteAll();
        savingsEntryRepository.deleteAll();
        savingsAccountRepository.deleteAll();
        savingsAccountTypeRepository.deleteAll();
        creditRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();

        // Create a property
        Property prop = new Property();
        prop.setLabel("Appartement Lyon");
        prop.setPurchaseValue(BigDecimal.valueOf(200000));
        prop.setPurchaseDate(LocalDate.of(2020, 1, 15));
        prop.setCurrentValue(BigDecimal.valueOf(240000));
        propertyRepository.save(prop);

        // Create a credit linked to the property
        Credit credit = new Credit();
        credit.setLabel("Prêt Lyon");
        credit.setType("Immobilier");
        credit.setTotalAmount(BigDecimal.valueOf(180000));
        credit.setRate(BigDecimal.valueOf(1.2));
        credit.setStartDate(LocalDate.of(2020, 2, 1));
        credit.setEndDate(LocalDate.of(2045, 2, 1));
        credit.setMonthlyPayment(BigDecimal.valueOf(750));
        credit.setRemainingAmount(BigDecimal.valueOf(160000));
        credit.setRemainingAmountDate(LocalDate.of(2025, 6, 1));
        credit.setProperty(prop);
        creditRepository.save(credit);

        // Create a credit NOT linked to any property
        Credit creditNoProperty = new Credit();
        creditNoProperty.setLabel("Prêt conso");
        creditNoProperty.setType("Consommation");
        creditNoProperty.setTotalAmount(BigDecimal.valueOf(5000));
        creditNoProperty.setRate(BigDecimal.valueOf(4.0));
        creditNoProperty.setStartDate(LocalDate.of(2024, 1, 1));
        creditNoProperty.setEndDate(LocalDate.of(2026, 1, 1));
        creditNoProperty.setMonthlyPayment(BigDecimal.valueOf(220));
        creditNoProperty.setRemainingAmount(BigDecimal.valueOf(2000));
        creditNoProperty.setRemainingAmountDate(LocalDate.of(2025, 6, 1));
        creditNoProperty.setProperty(null);
        creditRepository.save(creditNoProperty);

        ImportExportService svc = makeService();

        // Export
        ExportDto exported = svc.export();
        assertEquals(1, exported.getProperties().size());
        assertEquals(2, exported.getCredits().size());

        // Import (round trip)
        svc.importData(exported);

        // Verify property-credit link restored
        assertEquals(1, propertyRepository.count());
        assertEquals(2, creditRepository.count());

        List<Credit> credits = creditRepository.findAll();

        Credit linkedCredit = credits.stream()
                .filter(c -> "Prêt Lyon".equals(c.getLabel())).findFirst().orElseThrow();
        assertNotNull(linkedCredit.getProperty());
        assertEquals("Appartement Lyon", linkedCredit.getProperty().getLabel());
        assertEquals(0, BigDecimal.valueOf(240000).compareTo(linkedCredit.getProperty().getCurrentValue()));

        Credit unlinkedCredit = credits.stream()
                .filter(c -> "Prêt conso".equals(c.getLabel())).findFirst().orElseThrow();
        assertNull(unlinkedCredit.getProperty());
    }
}
