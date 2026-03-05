package com.example.internship.controllers;

import com.example.internship.models.Role;
import com.example.internship.models.University;
import com.example.internship.models.User;
import com.example.internship.repositories.UniversityRepository;
import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UniversityRepository universityRepository;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        // Загружаем все университеты из базы данных
        model.addAttribute("universities", universityRepository.findAll());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user,
                               @RequestParam("roleType") String roleType,
                               @RequestParam(value = "universityId", required = false) Long universityId,
                               @RequestParam(value = "uniName", required = false) String uniName,
                               Model model) {

        // Проверка на существующий логин
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("error", "Этот логин уже занят!");
            model.addAttribute("universities", universityRepository.findAll());
            return "register";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if ("UNIVERSITY".equalsIgnoreCase(roleType)) {
            // Логика для регистрации НОВОГО ВУЗа админом
            user.setRole(Role.UNIVERSITY_ADMIN);
            if (uniName != null && !uniName.trim().isEmpty()) {
                University newUni = new University();
                newUni.setName(uniName.trim());
                universityRepository.save(newUni);
                user.setUniversity(newUni);
            }
        } else if ("STUDENT".equalsIgnoreCase(roleType)) {
            // Логика для СТУДЕНТА: привязка к СУЩЕСТВУЮЩЕМУ ВУЗу
            user.setRole(Role.STUDENT);
            if (universityId != null) {
                universityRepository.findById(universityId).ifPresent(user::setUniversity);
            }
        } else if ("COMPANY".equalsIgnoreCase(roleType)) {
            user.setRole(Role.COMPANY);
        }

        userRepository.save(user);
        return "redirect:/login?success";
    }
}