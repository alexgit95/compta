package com.example.demo.repository;

import com.example.demo.model.SavingsAccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavingsAccountTypeRepository extends JpaRepository<SavingsAccountType, Long> {
    Optional<SavingsAccountType> findByName(String name);
}
