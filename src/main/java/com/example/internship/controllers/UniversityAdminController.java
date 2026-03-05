package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UniversityRepository;
import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/university-admin") // Все пути начинаются с этого префикса
public class UniversityAdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private InternshipRepository internshipRepository;

    @GetMapping("/dashboard") // Теперь полный путь будет: /university-admin/dashboard
    public String dashboard(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        University university = user.getUniversity();
        List<Internship> myInternships = internshipRepository.findByUniversity(university);

        // Собираем отклики
        List<Application> allApplications = myInternships.stream()
                .flatMap(i -> i.getApplications().stream())
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("university", university);
        model.addAttribute("internships", myInternships);
        model.addAttribute("allApplications", allApplications);

        // Файл должен лежать в: src/main/resources/templates/university/dashboard.html
        return "university/dashboard";
    }

    @PostMapping("/add-internship") // Убедись, что в HTML форме action="/university-admin/add-internship"
    public String addInternship(@ModelAttribute Internship internship, Principal principal) {
        // 1. Находим текущего представителя ВУЗа
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        // 2. Привязываем стажировку к его университету
        internship.setUniversity(user.getUniversity());

        // 3. Устанавливаем статус PENDING (На модерации)
        // Теперь она не появится у студентов, пока ГЛАВНЫЙ АДМИН ее не одобрит
        internship.setStatus(InternshipStatus.PENDING);

        // 4. Сохраняем
        internshipRepository.save(internship);

        // Редирект обратно на дашборд с сообщением об успехе
        return "redirect:/university-admin/dashboard?sentForModeration";
    }


}
