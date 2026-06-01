package com.example.demo.controller;

import com.example.demo.service.PatrimoineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/patrimoine")
@RequiredArgsConstructor
public class PatrimoineController {

    private final PatrimoineService patrimoineService;

    @GetMapping
    public String patrimoine(
            @RequestParam(defaultValue = "tendance") String mode,
            @RequestParam(defaultValue = "12") int duree,
            Model model) {
        model.addAttribute("grossPatrimoine", patrimoineService.getGrossPatrimoine());
        model.addAttribute("netPatrimoine", patrimoineService.getNetPatrimoine());
        model.addAttribute("propertyValue", patrimoineService.getTotalPropertyValue());
        model.addAttribute("savingsValue", patrimoineService.getTotalSavingsValue());

        Map<String, Object> chartData = patrimoineService.getChartData(mode, duree);
        model.addAttribute("chartLabels", chartData.get("labels"));
        model.addAttribute("chartGrossData", chartData.get("grossData"));
        model.addAttribute("chartNetData", chartData.get("netData"));

        model.addAttribute("projectionRows", patrimoineService.getProjectionRows(mode, duree));

        model.addAttribute("selectedMode", mode);
        model.addAttribute("selectedDuree", duree);
        model.addAttribute("activePage", "patrimoine");
        return "patrimoine";
    }
}
