package com.example.demo.controller;

import com.example.demo.dto.ExportDto;
import com.example.demo.model.*;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.service.*;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final ApiKeyService apiKeyService;
    private final ImportExportService importExportService;
    private final SavingsAccountTypeService savingsAccountTypeService;
    private final PropertyService propertyService;
    private final AppSettingService appSettingService;
    private final ObjectMapper objectMapper;

    // --- Categories ---

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("category", new Category());
        return "admin/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category, RedirectAttributes ra) {
        categoryRepository.save(category);
        ra.addFlashAttribute("success", "Catégorie enregistrée.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        categoryRepository.deleteById(id);
        ra.addFlashAttribute("success", "Catégorie supprimée.");
        return "redirect:/admin/categories";
    }

    // --- Users ---

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                             @RequestParam Role role,
                             RedirectAttributes ra) {
        String rawPassword = userService.createUser(username, role);
        ra.addFlashAttribute("newPassword", rawPassword);
        ra.addFlashAttribute("newUsername", username);
        ra.addFlashAttribute("success", "Utilisateur créé.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.deleteUser(id);
        ra.addFlashAttribute("success", "Utilisateur supprimé.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam Role role, RedirectAttributes ra) {
        userService.updateRole(id, role);
        ra.addFlashAttribute("success", "Rôle mis à jour.");
        return "redirect:/admin/users";
    }

    // --- API Keys ---

    @GetMapping("/apikeys")
    public String apiKeys(Model model) {
        model.addAttribute("apiKeys", apiKeyService.findAll());
        return "admin/apikeys";
    }

    @PostMapping("/apikeys/create")
    public String createApiKey(@RequestParam String name,
                               @RequestParam int validityDays,
                               RedirectAttributes ra) {
        String rawKey = apiKeyService.createKey(name, validityDays);
        ra.addFlashAttribute("newApiKey", rawKey);
        ra.addFlashAttribute("success", "Clé API créée.");
        return "redirect:/admin/apikeys";
    }

    @PostMapping("/apikeys/{id}/revoke")
    public String revokeApiKey(@PathVariable Long id, RedirectAttributes ra) {
        apiKeyService.revoke(id);
        ra.addFlashAttribute("success", "Clé révoquée.");
        return "redirect:/admin/apikeys";
    }

    // --- Import / Export ---

    @GetMapping("/data")
    public String dataPage(jakarta.servlet.http.HttpServletRequest request, Model model) {
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort();
        model.addAttribute("apiBaseUrl", baseUrl);
        return "admin/data";
    }

    @GetMapping("/data/export")
    public void exportData(HttpServletResponse response) throws IOException {
        ExportDto dto = importExportService.export();
        response.setContentType("application/json");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"budget-export-" + LocalDateTime.now().toLocalDate() + ".json\"");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(response.getOutputStream(), dto);
    }

    @PostMapping("/data/import")
    public String importData(@RequestParam("file") MultipartFile file, RedirectAttributes ra) throws IOException {
        ExportDto dto = objectMapper.readValue(file.getInputStream(), ExportDto.class);
        importExportService.importData(dto);
        ra.addFlashAttribute("success", "Import effectué avec succès.");
        return "redirect:/admin/data";
    }

    // --- Account Types ---

    @GetMapping("/account-types")
    public String accountTypes(Model model) {
        model.addAttribute("accountTypes", savingsAccountTypeService.findAll());
        model.addAttribute("accountType", new SavingsAccountType());
        return "admin/account-types";
    }

    @PostMapping("/account-types/save")
    public String saveAccountType(@ModelAttribute SavingsAccountType type, RedirectAttributes ra) {
        savingsAccountTypeService.save(type);
        ra.addFlashAttribute("success", "Type de support enregistré.");
        return "redirect:/admin/account-types";
    }

    @PostMapping("/account-types/{id}/delete")
    public String deleteAccountType(@PathVariable Long id, RedirectAttributes ra) {
        savingsAccountTypeService.delete(id);
        ra.addFlashAttribute("success", "Type de support supprimé.");
        return "redirect:/admin/account-types";
    }

    // --- Properties (Biens immobiliers) ---

    @GetMapping("/properties")
    public String properties(Model model) {
        model.addAttribute("properties", propertyService.findAll());
        model.addAttribute("property", new Property());
        return "admin/properties";
    }

    @PostMapping("/properties/save")
    public String saveProperty(@ModelAttribute Property property, RedirectAttributes ra) {
        propertyService.save(property);
        ra.addFlashAttribute("success", "Bien immobilier enregistré.");
        return "redirect:/admin/properties";
    }

    @PostMapping("/properties/{id}/delete")
    public String deleteProperty(@PathVariable Long id, RedirectAttributes ra) {
        propertyService.delete(id);
        ra.addFlashAttribute("success", "Bien immobilier supprimé.");
        return "redirect:/admin/properties";
    }

    // --- App Settings ---

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("salary",
                appSettingService.getNumeric(AppSettingService.KEY_BUDGET_SALARY, BigDecimal.ZERO));
        model.addAttribute("courses",
                appSettingService.getNumeric(AppSettingService.KEY_BUDGET_COURSES, BigDecimal.ZERO));
        return "admin/settings";
    }

    @PostMapping("/settings/save")
    public String saveSettings(@RequestParam BigDecimal salary,
                               @RequestParam BigDecimal courses,
                               RedirectAttributes ra) {
        appSettingService.saveNumeric(AppSettingService.KEY_BUDGET_SALARY,
                "Salaire mensuel total (€)", salary);
        appSettingService.saveNumeric(AppSettingService.KEY_BUDGET_COURSES,
                "Budget courses mensuelles (€)", courses);
        ra.addFlashAttribute("success", "Paramètres enregistrés.");
        return "redirect:/admin/settings";
    }
}
