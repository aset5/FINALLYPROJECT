package com.example.internship.controllers;

import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.repositories.InternshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private final InternshipRepository internshipRepository;

    public AdminController(InternshipRepository internshipRepository) {
        this.internshipRepository = internshipRepository;
    }
    // 1. Отображение всех вакансий (уже должно быть)
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        // Получаем все вакансии
        List<Internship> allJobs = internshipRepository.findAll();

        // Передаем в модель именно под тем именем, которое ждет HTML!
        model.addAttribute("allInternships", allJobs);

        // Если тебе нужен отдельный список для модерации
        List<Internship> pending = allJobs.stream()
                .filter(i -> i.getStatus() == InternshipStatus.PENDING)
                .toList();
        model.addAttribute("pendingInternships", pending);

        return "admin/dashboard";
    }

    // 2. CREATE: Добавление новой вакансии
    @PostMapping("/add")
    public String addInternship(@ModelAttribute Internship internship) {
        internship.setStatus(InternshipStatus.APPROVED); // Сразу одобряем для теста
        internshipRepository.save(internship);
        return "redirect:/admin/dashboard";
    }

    // 3. DELETE: Удаление по ID
    @PostMapping("/delete/{id}")
    public String deleteInternship(@PathVariable Long id) {
        internshipRepository.deleteById(id);
        return "redirect:/admin/dashboard";
    }

    // 4. UPDATE: Редактирование (простой вариант через сохранение)
    @PostMapping("/edit")
    public String editInternship(@ModelAttribute Internship internship) {
        // save() в JPA работает и на создание, и на обновление, если ID совпадает
        internshipRepository.save(internship);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/approve/{id}")
    public String approve(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        internship.setStatus(InternshipStatus.APPROVED); // ОДОБРЯЕМ
        internshipRepository.save(internship);
        return "redirect:/admin/dashboard";
    }

}