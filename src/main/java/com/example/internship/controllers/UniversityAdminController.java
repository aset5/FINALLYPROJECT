package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UniversityRepository;
import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/add-internship")
    public String addInternship(@ModelAttribute Internship internship, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        internship.setUniversity(user.getUniversity());
        internship.setStatus(InternshipStatus.PENDING);

        internshipRepository.save(internship);

        // ИСПРАВЛЕНО: добавляем префикс -admin
        return "redirect:/university-admin/dashboard";    }
    // 1. Показать форму редактирования
    @GetMapping("/internship/edit/{id}")
    public String editInternship(@PathVariable Long id, Model model) {
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Стажировка не найдена"));

        model.addAttribute("internship", internship);
        return "university/edit-internship"; // Путь к файлу формы
    }

    // 2. Сохранить изменения и отправить на модерацию
    @PostMapping("/internship/update")
    public String updateInternship(@ModelAttribute("internship") Internship internship) {
        // 1. Загружаем существующую из базы
        Internship existing = internshipRepository.findById(internship.getId()).get();

        // 2. Обновляем поля из формы
        existing.setTitle(internship.getTitle());
        existing.setDescription(internship.getDescription());
        existing.setStudyMaterials(internship.getStudyMaterials());
        existing.setMaxPlaces(internship.getMaxPlaces());

        // 3. СБРОС СТАТУСА — самое главное условие
        existing.setStatus(InternshipStatus.PENDING);

        internshipRepository.save(existing);

        // Редирект обратно в кабинет с уведомлением
        return "redirect:/university-admin/dashboard";    }



}
