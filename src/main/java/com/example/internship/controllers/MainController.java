package com.example.internship.controllers;

import com.example.internship.models.InternshipStatus;
import com.example.internship.repositories.InternshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @Autowired
    private InternshipRepository internshipRepository;

    @GetMapping("/")
    public String home(Model model) {
        // Показываем только одобренные вакансии
        model.addAttribute("internships", internshipRepository.findByStatus(InternshipStatus.APPROVED));
        return "index"; // Плитки (как на твоем втором скрине)
    }
}
