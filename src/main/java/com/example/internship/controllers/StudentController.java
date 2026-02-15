package com.example.internship.controllers;

import com.example.internship.models.Application;
import com.example.internship.models.Internship;
import com.example.internship.models.User;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final InternshipRepository internshipRepository;

    public StudentController(ApplicationRepository applicationRepository,
                             UserRepository userRepository,
                             InternshipRepository internshipRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.internshipRepository = internshipRepository;
    }

    @GetMapping("/my-applications")
    public String showMyApps(Model model, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Теперь метод findByStudent существует в репозитории
        List<Application> apps = applicationRepository.findByStudent(student);

        model.addAttribute("myApplications", apps);
        return "student/application";
    }

    @PostMapping("/apply/{internshipId}")
    public String apply(@PathVariable Long internshipId, Principal principal) {
        // 1. Ищем вакансию
        Internship internship = internshipRepository.findById(internshipId).orElse(null);
        if (internship == null) {
            return "redirect:/?error=not_found";
        }

        // 2. Находим студента
        User student = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Студент не найден"));

        // 3. Создаем отклик
        Application app = new Application();
        app.setStudent(student);
        app.setInternship(internship);
        app.setAppliedAt(LocalDateTime.now());

        applicationRepository.save(app);

        return "redirect:/?success";
    }

    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "student/profile";
    }

    @PostMapping("/profile/update")
    public String updateResume(@RequestParam("resume") String resume, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setResume(resume);
        userRepository.save(user);
        return "redirect:/student/profile?success";
    }


}