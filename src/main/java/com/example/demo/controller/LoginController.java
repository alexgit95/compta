package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request) {
        model.addAttribute("contextPath", request.getContextPath());
        return "login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/budget";
    }
}
