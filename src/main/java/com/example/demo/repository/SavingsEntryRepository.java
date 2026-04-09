package com.example.demo.repository;

import com.example.demo.model.SavingsAccount;
import com.example.demo.model.SavingsEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavingsEntryRepository extends JpaRepository<SavingsEntry, Long> {
    List<SavingsEntry> findBySavingsAccountOrderByEntryDateAsc(SavingsAccount account);
    Optional<SavingsEntry> findTopBySavingsAccountOrderByEntryDateDesc(SavingsAccount account);
    List<SavingsEntry> findBySavingsAccount(SavingsAccount account);
}
