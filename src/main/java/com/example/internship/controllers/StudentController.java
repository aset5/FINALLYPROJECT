package com.example.internship.controllers;

import com.example.internship.models.Application;
import com.example.internship.models.Internship;
import com.example.internship.models.User;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.InternshipRepository; // Нужно импортировать
import com.example.internship.repositories.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final InternshipRepository internshipRepository; // 1. Добавили поле

    // 2. Добавили репозиторий в конструктор для внедрения зависимостей (Dependency Injection)
    public StudentController(ApplicationRepository applicationRepository,
                             UserRepository userRepository,
                             InternshipRepository internshipRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.internshipRepository = internshipRepository;
    }

    @GetMapping("/my-applications") // убедись, что URL верный
    public String showMyApps(Model model, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username).orElseThrow();

        // Получаем отклики
        List<Application> apps = applicationRepository.findByStudent(student);

        // ВАЖНО: имя атрибута "myApplications"
        model.addAttribute("myApplications", apps);

        return "student/application";
    }

    @PostMapping("/apply/{internshipId}")
    public String apply(@PathVariable Long internshipId, Principal principal) {
        // 1. Проверяем, существует ли такая вакансия ВООБЩЕ
        Internship internship = internshipRepository.findById(internshipId).orElse(null);

        if (internship == null) {
            // Если вакансии нет (удалена), просто редиректим с ошибкой
            return "redirect:/?error=not_found";
        }

        // 2. Находим студента
        User student = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 3. Создаем отклик
        Application app = new Application();
        app.setStudent(student);
        app.setInternship(internship);
        app.setAppliedAt(java.time.LocalDateTime.now());

        applicationRepository.save(app);

        return "redirect:/?success";
    }
}