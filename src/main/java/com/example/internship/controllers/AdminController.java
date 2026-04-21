package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final InternshipRepository internshipRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminController(InternshipRepository internshipRepository,
                           UserRepository userRepository,
                           ApplicationRepository applicationRepository,
                           CompanyRepository companyRepository,
                           MessageRepository messageRepository,
                           PasswordEncoder passwordEncoder) {
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
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
        model.addAttribute("pendingInternships", internshipRepository.findByStatus(InternshipStatus.PENDING));
        return "admin/dashboard";
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

    @PostMapping("/users/update-full")
    public String updateFullUser(@RequestParam Long userId,
                                 @RequestParam String newUsername,
                                 @RequestParam(required = false) String newPassword,
                                 RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElseThrow();

        if (!user.getUsername().equals(newUsername) && userRepository.existsByUsername(newUsername)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Логин '" + newUsername + "' уже занят!");
            return "redirect:/admin/users";
        }
        user.setUsername(newUsername);

        if (newPassword != null && !newPassword.isBlank()) {
            // ЖАҢАРТЫЛҒАН REGEX: 8 символ + 1 Үлкен әріп
            String passwordPattern = "^(?=.*[A-Z]).{8,}$";
            if (!newPassword.matches(passwordPattern)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Пароль должен быть не менее 8 символов и содержать хотя бы одну заглавную букву!");
                return "redirect:/admin/users";
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Данные пользователя #" + userId + " успешно обновлены.");
        return "redirect:/admin/users";
    }

    @Transactional
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        messageRepository.deleteBySenderIdOrReceiverId(id, id);
        applicationRepository.deleteByStudent(user);

        Company company = companyRepository.findByUserId(user.getId());
        if (company != null) {
            List<Internship> companyJobs = internshipRepository.findByCompany(company);
            for (Internship job : companyJobs) {
                messageRepository.deleteByInternship(job);
                applicationRepository.deleteByInternship(job);
            }
            internshipRepository.deleteByCompany(company);
            companyRepository.delete(company);
        }
        userRepository.delete(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/approve/{id}")
    public String approveInternship(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        internship.setStatus(InternshipStatus.APPROVED);
        internshipRepository.save(internship);
        return "redirect:/admin/dashboard";
    }
}