package com.example.internship.controllers;

import com.example.internship.models.Company;
import com.example.internship.models.User;
import com.example.internship.repositories.CompanyRepository;
import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user) {
        // Проверка: не занято ли имя пользователя
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return "redirect:/register?error=user_exists";
        }

        // 1. Шифруем пароль
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Сохраняем пользователя в БД
        userRepository.save(user);

        // 3. Если роль - КОМПАНИЯ, создаем пустой профиль компании
        if ("ROLE_COMPANY".equals(user.getRole())) {
            Company company = new Company();
            company.setName("Компания: " + user.getUsername());
            company.setUser(user); // Теперь этот метод существует!
            companyRepository.save(company);
        }

        return "redirect:/login?success";
    }
}