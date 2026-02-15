package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.*;
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
    private final CompanyRepository companyRepository;
    private final MessageRepository messageRepository; // Добавлено

    @Autowired
    public AdminController(InternshipRepository internshipRepository,
                           UserRepository userRepository,
                           ApplicationRepository applicationRepository,
                           CompanyRepository companyRepository,
                           MessageRepository messageRepository) { // Добавлено
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model, @RequestParam(value = "keyword", required = false) String keyword) {
        List<Internship> internships;
        if (keyword != null && !keyword.isEmpty()) {
            internships = internshipRepository.findByTitleContainingIgnoreCaseOrCityContainingIgnoreCase(keyword, keyword);
            model.addAttribute("keyword", keyword);
        } else {
            internships = internshipRepository.findAll();
        }
        model.addAttribute("allInternships", internships);
        return "admin/dashboard";
    }

    @PostMapping("/delete/{id}")
    @Transactional
    public String deleteInternship(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        // Сначала удаляем сообщения и отклики, связанные с вакансией
        messageRepository.deleteByInternship(internship);
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
    public String listUsers(Model model, @RequestParam(value = "keyword", required = false) String keyword) {
        List<User> users;
        if (keyword != null && !keyword.isEmpty()) {
            users = userRepository.findByUsernameContainingIgnoreCase(keyword);
            model.addAttribute("keyword", keyword);
        } else {
            users = userRepository.findAll();
        }
        model.addAttribute("users", users);
        return "admin/users";
    }

    @Transactional
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();

        // 1. Удаляем ВСЕ сообщения, где этот пользователь — отправитель или получатель
        messageRepository.deleteBySenderIdOrReceiverId(id, id);

        // 2. Удаляем отклики самого пользователя (если он студент)
        applicationRepository.deleteByStudent(user);

        // 3. Если это компания, чистим всё, что с ней связано
        Company company = companyRepository.findByUserId(user.getId());
        if (company != null) {
            List<Internship> companyJobs = internshipRepository.findByCompany(company);
            for (Internship job : companyJobs) {
                // Удаляем сообщения и отклики по каждой вакансии компании
                messageRepository.deleteByInternship(job);
                applicationRepository.deleteByInternship(job);
            }
            // Удаляем сами вакансии
            internshipRepository.deleteByCompany(company);
            // Удаляем профиль компании
            companyRepository.delete(company);
        }

        // 4. Теперь, когда нет никаких связей в других таблицах, удаляем пользователя
        userRepository.delete(user);

        return "redirect:/admin/users";
    }
}