package com.example.demo.service;

import com.example.demo.model.Credit;
import com.example.demo.repository.CreditRepository;
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
class CreditServiceTest {

    @Autowired
    private CreditService creditService;

    @Autowired
    private CreditRepository creditRepository;

    @Test
    void saveAndFindCredit() {
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

        Credit saved = creditService.save(credit);
        assertNotNull(saved.getId());

        List<Credit> all = creditService.findAll();
        assertEquals(1, all.size());
        assertEquals("Prêt immobilier", all.get(0).getLabel());
    }

    @Test
    void deleteCredit() {
        Credit credit = new Credit();
        credit.setLabel("Prêt auto");
        credit.setType("Automobile");
        credit.setTotalAmount(BigDecimal.valueOf(15000));
        credit.setRate(BigDecimal.valueOf(3.5));
        credit.setStartDate(LocalDate.of(2023, 1, 1));
        credit.setEndDate(LocalDate.of(2028, 1, 1));
        credit.setMonthlyPayment(BigDecimal.valueOf(280));
        credit.setRemainingAmount(BigDecimal.valueOf(10000));
        credit.setRemainingAmountDate(LocalDate.now());

        Credit saved = creditService.save(credit);
        creditService.delete(saved.getId());

        assertEquals(0, creditRepository.count());
    }

    @Test
    void getRepaymentPercentage() {
        Credit credit = new Credit();
        credit.setLabel("Test");
        credit.setType("Consommation");
        credit.setTotalAmount(BigDecimal.valueOf(10000));
        credit.setRate(BigDecimal.valueOf(2.0));
        credit.setStartDate(LocalDate.of(2022, 1, 1));
        credit.setEndDate(LocalDate.of(2027, 1, 1));
        credit.setMonthlyPayment(BigDecimal.valueOf(180));
        credit.setRemainingAmount(BigDecimal.valueOf(4000));
        credit.setRemainingAmountDate(LocalDate.now());

        // Paid 6000 out of 10000 = 60%
        BigDecimal pct = creditService.getRepaymentPercentage(credit);
        assertEquals(0, BigDecimal.valueOf(60.0).compareTo(pct));
    }

    @Test
    void getRemainingDurationLabel() {
        Credit credit = new Credit();
        credit.setLabel("Test");
        credit.setType("Immobilier");
        credit.setTotalAmount(BigDecimal.valueOf(100000));
        credit.setRate(BigDecimal.valueOf(1.0));
        credit.setStartDate(LocalDate.of(2020, 1, 1));
        credit.setEndDate(LocalDate.now().plusYears(2).plusMonths(3));
        credit.setMonthlyPayment(BigDecimal.valueOf(500));
        credit.setRemainingAmount(BigDecimal.valueOf(50000));
        credit.setRemainingAmountDate(LocalDate.now());

        String label = creditService.getRemainingDurationLabel(credit);
        assertTrue(label.contains("2 ans"));
        assertTrue(label.contains("mois"));
    }

    @Test
    void getRemainingDurationLabelWhenExpired() {
        Credit credit = new Credit();
        credit.setLabel("Terminé");
        credit.setType("Consommation");
        credit.setTotalAmount(BigDecimal.valueOf(5000));
        credit.setRate(BigDecimal.valueOf(4.0));
        credit.setStartDate(LocalDate.of(2018, 1, 1));
        credit.setEndDate(LocalDate.of(2022, 1, 1));
        credit.setMonthlyPayment(BigDecimal.valueOf(120));
        credit.setRemainingAmount(BigDecimal.ZERO);
        credit.setRemainingAmountDate(LocalDate.now());

        String label = creditService.getRemainingDurationLabel(credit);
        assertEquals("0 mois", label);
    }

    @Test
    void getCurrentRemainingAmountSubtractsElapsedMonthlyPayments() {
        Credit credit = new Credit();
        credit.setLabel("Test restant");
        credit.setType("Immobilier");
        credit.setTotalAmount(BigDecimal.valueOf(100000));
        credit.setRate(BigDecimal.valueOf(1.0));
        credit.setStartDate(LocalDate.of(2020, 1, 1));
        credit.setEndDate(LocalDate.of(2040, 1, 1));
        credit.setMonthlyPayment(BigDecimal.valueOf(500));
        credit.setRemainingAmount(BigDecimal.valueOf(10000));
        credit.setRemainingAmountDate(LocalDate.now().minusMonths(3));

        BigDecimal remaining = creditService.getCurrentRemainingAmount(credit);
        assertEquals(0, BigDecimal.valueOf(8500.00).compareTo(remaining));
    }

    @Test
    void getCurrentRemainingAmountDoesNotGoBelowZero() {
        Credit credit = new Credit();
        credit.setLabel("Test zéro");
        credit.setType("Consommation");
        credit.setTotalAmount(BigDecimal.valueOf(5000));
        credit.setRate(BigDecimal.ZERO);
        credit.setStartDate(LocalDate.of(2024, 1, 1));
        credit.setEndDate(LocalDate.of(2026, 1, 1));
        credit.setMonthlyPayment(BigDecimal.valueOf(300));
        credit.setRemainingAmount(BigDecimal.valueOf(600));
        credit.setRemainingAmountDate(LocalDate.now().minusMonths(3));

        BigDecimal remaining = creditService.getCurrentRemainingAmount(credit);
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(remaining));
    }
}
