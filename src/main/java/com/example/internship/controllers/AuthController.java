package com.example.internship.controllers;

import com.example.internship.models.Role;
import com.example.internship.models.University;
import com.example.internship.models.User;
import com.example.internship.repositories.UniversityRepository;
import com.example.internship.repositories.UserRepository;
import com.example.internship.services.EmailService; // Жаңа сервис
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private EmailService emailService; // Хат жіберу үшін

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("universities", universityRepository.findAll());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult bindingResult,
                               @RequestParam("roleType") String roleType,
                               @RequestParam(value = "universityId", required = false) Long universityId,
                               @RequestParam(value = "uniName", required = false) String uniName,
                               Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("universities", universityRepository.findAll());
            return "register";
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("error", "Бұл логин бос емес!");
            model.addAttribute("universities", universityRepository.findAll());
            return "register";
        }

        // 1. Құпия сөзді шифрлау
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Растау кодын жасау және аккаунтты уақытша жабу
        String token = UUID.randomUUID().toString();
        user.setVerificationCode(token);
        user.setEnabled(false);

        // 3. Рөлдер мен Университет логикасы
        if ("UNIVERSITY".equalsIgnoreCase(roleType)) {
            user.setRole(Role.UNIVERSITY_ADMIN);
            if (uniName != null && !uniName.trim().isEmpty()) {
                University newUni = new University();
                newUni.setName(uniName.trim());
                universityRepository.save(newUni);
                user.setUniversity(newUni);
            }
        } else if ("STUDENT".equalsIgnoreCase(roleType)) {
            user.setRole(Role.STUDENT);
            if (universityId != null) {
                universityRepository.findById(universityId).ifPresent(user::setUniversity);
            }
        } else if ("COMPANY".equalsIgnoreCase(roleType)) {
            user.setRole(Role.COMPANY);
        }

        try {
            userRepository.save(user);
            // 4. Пайдаланушыға хат жіберу
            emailService.sendVerificationEmail(user.getEmail(), token);
        } catch (Exception e) {
            model.addAttribute("error", "Хат жіберу кезінде техникалық қате шықты: " + e.getMessage());
            return "register";
        }

        return "redirect:/login?sent";
    }

    // 5. Поштадағы сілтемені басқанда осы әдіс жұмыс істейді
    @GetMapping("/verify")
    public String verifyUser(@RequestParam("code") String code) {
        User user = userRepository.findByVerificationCode(code);
        if (user != null) {
            user.setEnabled(true);
            user.setVerificationCode(null); // Кодты бір рет қана қолдану үшін
            userRepository.save(user);
            return "redirect:/login?verified";
        } else {
            return "redirect:/login?error";
        }
    }
}