package com.example.internship.controllers;

import com.example.internship.models.Role;
import com.example.internship.models.User;
import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
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

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register"; // Должен вернуть файл register.html из папки templates
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, @RequestParam("roleType") String roleType) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if ("COMPANY".equals(roleType)) {
            user.setRole(Role.COMPANY); // Сохраняем как компанию
        } else {
            user.setRole(Role.STUDENT);
        }

        userRepository.save(user);
        return "redirect:/login";
    }
}