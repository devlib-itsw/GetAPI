package com.getapi.api.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class ApiController {
    @GetMapping("")
    public String index(Model model) {
        return "dashboard";
    }
    @GetMapping("/create")
    public String ApiCreate(Model model) {
        return "dashboard-create";
    }
}
