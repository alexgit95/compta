package com.example.demo.service;

import com.example.demo.model.SavingsAccountType;
import com.example.demo.repository.SavingsAccountTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsAccountTypeService {

    private final SavingsAccountTypeRepository repository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDefaultTypes() {
        List<SavingsAccountType> defaults = List.of(
            new SavingsAccountType("Épargne de précaution",       "🛟",  15),
            new SavingsAccountType("Livret (épargne liquide)",    "💧",  15),
            new SavingsAccountType("Fonds euros (assurance vie)", "🛡️", 25),
            new SavingsAccountType("Actions / PEA",               "📈",  25),
            new SavingsAccountType("Immobilier (SCPI/SCI)",       "🏠",  15),
            new SavingsAccountType("Crypto / Alternatif",         "₿",    5)
        );
        for (SavingsAccountType type : defaults) {
            if (repository.findByName(type.getName()).isEmpty()) {
                repository.save(type);
            }
        }
    }

    public List<SavingsAccountType> findAll() {
        return repository.findAll();
    }

    public SavingsAccountType findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional
    public SavingsAccountType save(SavingsAccountType type) {
        return repository.save(type);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
