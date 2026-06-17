package com.example.demo.config;

import com.example.demo.model.Role;
import com.example.demo.model.ShoppingSettings;
import com.example.demo.model.User;
import com.example.demo.repository.ShoppingSettingsRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShoppingSettingsRepository shoppingSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Default admin user created: {}", adminUsername);
        }

        // Initialize shopping settings with default values if empty
        if (shoppingSettingsRepository.count() == 0) {
            ShoppingSettings shopping = new ShoppingSettings(
                BigDecimal.valueOf(80.0),  // Default 80€ per shopping trip
                7,                          // Default weekly (7 days)
                LocalDate.now()             // Last shopping was today
            );
            shoppingSettingsRepository.save(shopping);
            log.info("Default shopping settings created: {} €, every {} days", 
                     shopping.getAmount(), shopping.getFrequencyDays());
        }
    }
}
