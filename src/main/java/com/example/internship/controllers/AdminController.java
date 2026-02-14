package com.example.internship.controllers;

import com.example.internship.models.Company;
import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.models.User;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UserRepository;
import com.example.internship.repositories.CompanyRepository; // 1. Добавлен импорт
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final InternshipRepository internshipRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository; // 2. Объявлено поле

    @Autowired
    public AdminController(InternshipRepository internshipRepository,
                           UserRepository userRepository,
                           ApplicationRepository applicationRepository,
                           CompanyRepository companyRepository) { // 3. Добавлено в конструктор
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository; // 4. Инициализация
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("allInternships", internshipRepository.findAll());
        return "admin/dashboard";
    }

    @PostMapping("/delete/{id}")
    @Transactional
    public String deleteInternship(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        applicationRepository.deleteByInternship(internship);
        internshipRepository.delete(internship);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/approve/{id}")
    public String approve(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        internship.setStatus(InternshipStatus.APPROVED);
        internshipRepository.save(internship);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    @Transactional
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();

        // 1. Удаляем отклики самого пользователя (если он студент)
        applicationRepository.deleteByStudent(user);

        // 2. Проверяем, есть ли у пользователя профиль компании
        Company company = companyRepository.findByUserId(user.getId());

        if (company != null) {
            List<Internship> companyJobs = internshipRepository.findByCompany(company);
            for (Internship job : companyJobs) {
                applicationRepository.deleteByInternship(job);
            }
            internshipRepository.deleteByCompany(company);
            companyRepository.delete(company);
        }

        // 3. Наконец, удаляем самого пользователя
        userRepository.delete(user);

        return "redirect:/admin/users";
    }
}