package com.example.demo.service;

import com.example.demo.model.SavingsAccount;
import com.example.demo.model.SavingsEntry;
import com.example.demo.repository.SavingsAccountRepository;
import com.example.demo.repository.SavingsEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SavingsService {

    private final SavingsAccountRepository accountRepository;
    private final SavingsEntryRepository entryRepository;

    public List<SavingsAccount> findAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional
    public SavingsAccount saveAccount(SavingsAccount account) {
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        SavingsAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        entryRepository.findBySavingsAccount(account).forEach(entryRepository::delete);
        accountRepository.delete(account);
    }

    public List<SavingsEntry> findEntriesForAccount(SavingsAccount account) {
        return entryRepository.findBySavingsAccountOrderByEntryDateAsc(account);
    }

    @Transactional
    public SavingsEntry addEntry(SavingsAccount account, BigDecimal balance, LocalDate date) {
        SavingsEntry entry = new SavingsEntry();
        entry.setSavingsAccount(account);
        entry.setBalance(balance);
        entry.setEntryDate(date);
        return entryRepository.save(entry);
    }

    @Transactional
    public void deleteEntry(Long entryId) {
        entryRepository.deleteById(entryId);
    }

    /**
     * Computes the projected balance for an account on a given target date.
     * Takes the most recent entry before or on targetDate and adds monthly deposits.
     * Comparisons are done at month granularity (day-of-month is ignored) so that
     * an entry on the 8th is correctly matched when the cursor is the 1st of the same month.
     */
    public BigDecimal projectBalance(SavingsAccount account, LocalDate targetDate) {
        List<SavingsEntry> entries = entryRepository.findBySavingsAccountOrderByEntryDateAsc(account);
        if (entries.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDate targetMonth = targetDate.withDayOfMonth(1);

        // Find the latest entry whose month is <= targetDate's month
        SavingsEntry reference = null;
        for (SavingsEntry entry : entries) {
            if (!entry.getEntryDate().withDayOfMonth(1).isAfter(targetMonth)) {
                reference = entry;
            }
        }
        if (reference == null) {
            // all entries are after targetDate — use the earliest
            reference = entries.get(0);
        }

        long months = ChronoUnit.MONTHS.between(reference.getEntryDate().withDayOfMonth(1), targetMonth);
        if (months < 0) months = 0;
        return reference.getBalance().add(account.getMonthlyDeposit().multiply(BigDecimal.valueOf(months)));
    }

    /**
     * Returns chart data: monthly projection points from `from` to `to`,
     * marking real entry months as such.
     */
    public List<Map<String, Object>> getChartData(SavingsAccount account, LocalDate from, LocalDate to) {
        List<SavingsEntry> entries = entryRepository.findBySavingsAccountOrderByEntryDateAsc(account);
        // Build a set of year-month that have a real entry
        Set<String> realMonths = new HashSet<>();
        for (SavingsEntry e : entries) {
            realMonths.add(e.getEntryDate().getYear() + "-" + e.getEntryDate().getMonthValue());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate cursor = from.withDayOfMonth(1);
        while (!cursor.isAfter(to)) {
            boolean isReal = realMonths.contains(cursor.getYear() + "-" + cursor.getMonthValue());
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", cursor.toString());
            point.put("balance", projectBalance(account, cursor));
            point.put("real", isReal);
            result.add(point);
            cursor = cursor.plusMonths(1);
        }
        return result;
    }
}
