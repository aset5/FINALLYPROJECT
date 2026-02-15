package com.example.internship.controllers;

import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.repositories.InternshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class MainController {
    @Autowired
    private InternshipRepository internshipRepository;

    @GetMapping("/")
    public String home(Model model, @RequestParam(value = "keyword", required = false) String keyword) {
        List<Internship> internships;

        if (keyword != null && !keyword.isEmpty()) {
            // Если есть поисковый запрос, ищем по нему
            internships = internshipRepository.findByStatusAndTitleContainingIgnoreCaseOrStatusAndDescriptionContainingIgnoreCase(
                    InternshipStatus.APPROVED, keyword,
                    InternshipStatus.APPROVED, keyword
            );
            model.addAttribute("keyword", keyword); // Возвращаем слово в поиск, чтобы оно не исчезало
        } else {
            // Если поиска нет, показываем все одобренные
            internships = internshipRepository.findByStatus(InternshipStatus.APPROVED);
        }

        model.addAttribute("internships", internships);
        return "index";
    }
}
