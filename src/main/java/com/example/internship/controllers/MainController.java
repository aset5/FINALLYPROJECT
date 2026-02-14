package com.example.internship.controllers;

import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.repositories.InternshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MainController {
    @Autowired
    private InternshipRepository internshipRepository;

    @GetMapping("/")
    public String home(Model model) {
        // Выбираем ТОЛЬКО те вакансии, которые одобрены админом
        List<Internship> approvedInternships = internshipRepository.findAll()
                .stream()
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .toList();

        model.addAttribute("internships", approvedInternships);
        return "index"; // Твоя главная страница
    }
}
