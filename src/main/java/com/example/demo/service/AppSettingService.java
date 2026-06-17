package com.example.demo.service;

import com.example.demo.model.AppSetting;
import com.example.demo.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppSettingService {

    /** Key: total monthly salary used as the Sankey source in Budget. */
    public static final String KEY_BUDGET_SALARY = "sankey.budget.salary";
    /** Key: monthly courses (groceries) budget used as a special Sankey node in Budget. */
    public static final String KEY_BUDGET_COURSES = "sankey.budget.courses";

    private final AppSettingRepository repository;

    public List<AppSetting> findAll() {
        return repository.findAll();
    }

    public BigDecimal getNumeric(String key, BigDecimal defaultValue) {
        return repository.findById(key)
                .map(AppSetting::getNumericValue)
                .filter(v -> v != null)
                .orElse(defaultValue);
    }

    @Transactional
    public void saveNumeric(String key, String label, BigDecimal value) {
        AppSetting setting = repository.findById(key)
                .orElseGet(() -> new AppSetting(key, label, value));
        setting.setLabel(label);
        setting.setNumericValue(value);
        repository.save(setting);
    }

    @Transactional
    public void ensureDefaults() {
        if (!repository.existsById(KEY_BUDGET_SALARY)) {
            repository.save(new AppSetting(KEY_BUDGET_SALARY, "Salaire mensuel total (€)", BigDecimal.ZERO));
        }
        if (!repository.existsById(KEY_BUDGET_COURSES)) {
            repository.save(new AppSetting(KEY_BUDGET_COURSES, "Budget courses mensuelles (€)", BigDecimal.ZERO));
        }
    }
}
