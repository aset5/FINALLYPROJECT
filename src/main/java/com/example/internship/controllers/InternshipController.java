package com.example.internship.controllers;

import com.example.internship.models.Company;
import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.models.User;
import com.example.internship.repositories.CompanyRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UserRepository;
import com.example.internship.services.InternshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
public class InternshipController {

    // Подключаем необходимые репозитории, чтобы ошибка "cannot find symbol" исчезла
    @Autowired
    private InternshipService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private InternshipRepository internshipRepository;

    // ГЛАВНАЯ: Видна всем, показывает только APPROVED
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("internships", service.getApprovedInternships());
        return "index";
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    // ДЕТАЛИ: Только для авторизованных
    @GetMapping("/internship/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("internship", service.getInternshipById(id));
        return "details";
    }

    // --- ДЛЯ КОМПАНИИ (Создание заявки) ---
    @GetMapping("/company/add")
    public String addForm(Model model) {
        model.addAttribute("internship", new Internship());
        return "add-internship";
    }

    @PostMapping("/company/add")
    public String create(@ModelAttribute Internship internship, Principal principal) {
        // 1. Получаем текущего пользователя (компанию)
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 2. Находим профиль компании
        Company company = companyRepository.findByUser(currentUser);

        // 3. Настройка вакансии
        internship.setCompany(company);

        // МГНОВЕННАЯ ПУБЛИКАЦИЯ: ставим статус APPROVED сразу
        internship.setStatus(InternshipStatus.APPROVED);

        internshipRepository.save(internship);

        // Перенаправляем на главную, где вакансия уже появится
        return "redirect:/?published=true";
    }

    // --- ДЛЯ АДМИНА (Публикация) ---
    @GetMapping("/admin/moderation")
    public String adminPanel(Model model) {
        model.addAttribute("pendingItems", service.getPendingInternships());
        return "admin-moderation";
    }

    @PostMapping("/admin/publish/{id}")
    public String publish(@PathVariable Long id) {
        service.approve(id); // Меняет статус на APPROVED
        return "redirect:/admin/moderation";
    }
}