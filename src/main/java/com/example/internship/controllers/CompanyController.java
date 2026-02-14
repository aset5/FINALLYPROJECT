package com.example.internship.controllers;

import com.example.internship.models.Company;
import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.models.User;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.CompanyRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UserRepository; // 1. Импорт
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/company")
public class CompanyController {

    private final InternshipRepository internshipRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;

    // ВНИМАНИЕ: Исправленный конструктор (все 4 репозитория)
    public CompanyController(InternshipRepository internshipRepository,
                             UserRepository userRepository,
                             ApplicationRepository applicationRepository,
                             CompanyRepository companyRepository) {
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
    }

    @GetMapping("/dashboard")
    public String companyDashboard(Model model, Principal principal) {
        // 1. Находим юзера
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Находим компанию этого юзера
        Company company = companyRepository.findByUserId(user.getId());
        if (company == null) {
            // Если компании нет в БД, создаем пустой список, чтобы не было ошибки в UI
            model.addAttribute("myInternships", java.util.Collections.emptyList());
            model.addAttribute("candidates", java.util.Collections.emptyList());
            return "company/dashboard";
        }

        // 3. Используем ID КОМПАНИИ для поиска вакансий и откликов
        model.addAttribute("myInternships", internshipRepository.findByCompanyId(company.getId()));
        model.addAttribute("candidates", applicationRepository.findByInternshipCompanyId(company.getId()));

        return "company/dashboard";
    }

    @PostMapping("/add")
    public String addInternship(Internship internship, Principal principal) {
        // 1. Находим пользователя
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Находим компанию
        Company company = companyRepository.findByUserId(user.getId());

        // ПРОВЕРКА ДОЛЖНА БЫТЬ ЗДЕСЬ
        if (company == null) {
            return "redirect:/company/setup-profile?error=no_company";
        }

        // 3. Установка данных и статуса PENDING (на проверку админу)
        internship.setCompany(company);
        internship.setStatus(InternshipStatus.PENDING);

        internshipRepository.save(internship);
        return "redirect:/company/dashboard";
    }

    @PostMapping("/delete/{id}")
    public String deleteInternship(@PathVariable Long id) {
        internshipRepository.deleteById(id);
        return "redirect:/company/dashboard";
    }

    // Страница с формой заполнения данных компании
    @GetMapping("/setup-profile")
    public String setupProfilePage(Model model) {
        model.addAttribute("company", new Company());
        return "company/setup-profile";
    }

    // Обработка формы создания компании
    @PostMapping("/setup-profile")
    public String saveProfile(@ModelAttribute Company company, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).get();
        company.setUser(user);
        companyRepository.save(company);
        return "redirect:/company/dashboard";
    }
}