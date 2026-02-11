package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.*;
import com.example.internship.services.InternshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller
public class InternshipController {

    @Autowired private InternshipService service;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private InternshipRepository internshipRepository;
    @Autowired private InternshipApplicationRepository internshipApplicationRepository;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("internships", service.getApprovedInternships());
        return "index";
    }

    @GetMapping("/internship/{id}")
    public String details(@PathVariable Long id, Model model) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        model.addAttribute("internship", internship);
        return "details"; // Убедитесь, что файл details.html существует
    }


    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/company/add")
    public String addForm(Model model) {
        model.addAttribute("internship", new Internship());
        return "add-internship";
    }

    @PostMapping("/company/add")
    public String create(@ModelAttribute Internship internship, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Company company = companyRepository.findByUser(user);
        internship.setCompany(company);
        internship.setStatus(InternshipStatus.APPROVED);
        internshipRepository.save(internship);
        return "redirect:/?published=true";
    }

    @PostMapping("/internship/apply/{id}")
    public String apply(@PathVariable Long id, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        // Проверка: только STUDENT может откликаться
        if (!user.getRole().contains("STUDENT")) {
            return "redirect:/?error=not_student";
        }

        Internship internship = internshipRepository.findById(id).orElseThrow();
        if (!internshipApplicationRepository.existsByStudentAndInternship(user, internship)) {
            InternshipApplicationModel app = new InternshipApplicationModel();
            app.setStudent(user);
            app.setInternship(internship);
            internshipApplicationRepository.save(app);
        }
        return "redirect:/?applied=true";
    }

    @GetMapping("/company/applications")
    public String viewApplications(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Company company = companyRepository.findByUser(user);
        model.addAttribute("apps", internshipApplicationRepository.findByInternship_Company(company));
        return "company-applications";
    }


}