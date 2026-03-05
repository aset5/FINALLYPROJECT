package com.example.internship.controllers;

import com.example.internship.models.User;
import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/university-admin") // Все пути начинаются с этого префикса
public class UniversityAdminController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        // Находим текущего админа и его ВУЗ
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        model.addAttribute("user", user);
        model.addAttribute("university", user.getUniversity());

        // Возвращает файл templates/university/dashboard.html
        return "university/dashboard";
    }
}
