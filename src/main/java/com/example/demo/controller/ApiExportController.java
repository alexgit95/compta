package com.example.demo.controller;

import com.example.demo.dto.ExportDto;
import com.example.demo.service.ApiKeyService;
import com.example.demo.service.ImportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiExportController {

    private final ImportExportService importExportService;

    @GetMapping("/export")
    @PreAuthorize("hasRole('API')")
    public ResponseEntity<ExportDto> export() {
        return ResponseEntity.ok(importExportService.export());
    }
}
