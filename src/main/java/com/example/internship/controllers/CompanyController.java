package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.CompanyRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections; // Импорт добавлен

@Controller
@RequestMapping("/company")
public class CompanyController {

    private final InternshipRepository internshipRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;

    public CompanyController(InternshipRepository internshipRepository,
                             UserRepository userRepository,
                             ApplicationRepository applicationRepository,
                             CompanyRepository companyRepository) {
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
    }

    @GetMapping("/dashboard")
    public String companyDashboard(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Company company = companyRepository.findByUserId(user.getId());
        if (company == null) {
            model.addAttribute("myInternships", Collections.emptyList());
            model.addAttribute("candidates", Collections.emptyList());
            return "company/dashboard";
        }

        // Теперь эти методы существуют в репозиториях:
        model.addAttribute("myInternships", internshipRepository.findByCompanyId(company.getId()));
        model.addAttribute("candidates", applicationRepository.findByInternshipCompanyId(company.getId()));

        return "company/dashboard";
    }

    @PostMapping("/add")
    public String addInternship(Internship internship, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Company company = companyRepository.findByUserId(user.getId());
        if (company == null) {
            return "redirect:/company/setup-profile?error=no_company";
        }

        internship.setCompany(company);
        internship.setStatus(InternshipStatus.PENDING);
        internshipRepository.save(internship);
        return "redirect:/company/dashboard";
    }

    @Transactional
    @PostMapping("/delete/{id}")
    public String deleteInternship(@PathVariable Long id) {
        // Удаляем отклики по ID вакансии (теперь метод есть в репозитории)
        applicationRepository.deleteByInternshipId(id);
        // Удаляем вакансию
        internshipRepository.deleteById(id);
        return "redirect:/company/dashboard";
    }

    @GetMapping("/setup-profile")
    public String setupProfilePage(Model model) {
        model.addAttribute("company", new Company());
        return "company/setup-profile";
    }

    @PostMapping("/setup-profile")
    public String saveProfile(@ModelAttribute Company company, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        company.setUser(user);
        companyRepository.save(company);
        return "redirect:/company/dashboard";
    }

    @GetMapping("/student-profile/{id}")
    public String viewStudentProfile(@PathVariable Long id, Model model) {
        User student = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Студент не найден"));
        model.addAttribute("student", student);
        return "company/student-view";
    }

    @PostMapping("/applications/{id}/accept") // Адрес: /company/applications/ID/accept
    public String acceptApplication(@PathVariable Long id) {
        Application app = applicationRepository.findById(id).orElseThrow();
        app.setStatus(ApplicationStatus.ACCEPTED);
        applicationRepository.save(app);
        return "redirect:/company/dashboard";
    }

    @PostMapping("/applications/{id}/reject")
    public String rejectApplication(@PathVariable Long id) {
        Application app = applicationRepository.findById(id).orElseThrow();
        app.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(app);
        return "redirect:/company/dashboard";
    }

    @PostMapping("/internships/{id}/close")
    public String closeInternship(@PathVariable Long id, Principal principal) {
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Вакансия не найдена"));

        // Проверка прав доступа
        if (!internship.getCompany().getUser().getUsername().equals(principal.getName())) {
            throw new AccessDeniedException("Вы не можете закрыть чужую вакансию!");
        }

        internship.setStatus(InternshipStatus.CLOSED);
        internshipRepository.save(internship);
        return "redirect:/company/dashboard";
    }

    @PostMapping("/internships/{id}/reopen")
    public String reopenInternship(@PathVariable Long id, Principal principal) {
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Вакансия не найдена"));

        // Проверка прав доступа
        if (!internship.getCompany().getUser().getUsername().equals(principal.getName())) {
            throw new AccessDeniedException("Вы не можете открыть чужую вакансию!");
        }

        internship.setStatus(InternshipStatus.APPROVED);
        internshipRepository.save(internship);
        return "redirect:/company/dashboard";
    }

    @GetMapping("/internships/edit/{id}")
    public String editInternshipPage(@PathVariable Long id, Model model, Principal principal) {
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Вакансия не найдена"));

        // Проверка прав (редактировать может только владелец)
        if (!internship.getCompany().getUser().getUsername().equals(principal.getName())) {
            throw new AccessDeniedException("Вы не можете редактировать чужую вакансию!");
        }

        model.addAttribute("internship", internship);
        return "company/edit-internship";
    }

    @PostMapping("/internships/edit/{id}")
    public String updateInternship(@PathVariable Long id,
                                   @ModelAttribute("internship") Internship updatedData,
                                   Principal principal) {
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Вакансия не найдена"));

        // Проверка прав доступа
        if (!internship.getCompany().getUser().getUsername().equals(principal.getName())) {
            throw new AccessDeniedException("Доступ запрещен");
        }

        // Обновляем поля
        internship.setTitle(updatedData.getTitle());
        internship.setCity(updatedData.getCity());
        internship.setDescription(updatedData.getDescription());

        // ГЛАВНОЕ: Сбрасываем статус на PENDING при любом изменении
        // Теперь вакансия исчезнет из общего списка у студентов и попадет в админку
        internship.setStatus(InternshipStatus.PENDING);

        internshipRepository.save(internship);

        // Можно добавить сообщение для пользователя, что вакансия ушла на модерацию
        return "redirect:/company/dashboard?msg=remoderation";
    }
}