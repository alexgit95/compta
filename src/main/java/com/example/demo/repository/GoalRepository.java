package com.example.demo.repository;

import com.example.demo.model.Goal;
import com.example.demo.model.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findBySavingsAccount(SavingsAccount account);
}
