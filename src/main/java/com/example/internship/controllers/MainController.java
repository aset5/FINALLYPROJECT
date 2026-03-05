package com.example.internship.controllers;

import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.models.Role;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class MainController {
    @Autowired
    private InternshipRepository internshipRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                model.addAttribute("currentUser", user);

                if (user.getRole() == Role.STUDENT && user.getUniversity() != null) {
                    // Студент видит только одобренные стажировки своего ВУЗа
                    model.addAttribute("internships",
                            internshipRepository.findByStatusAndUniversityId(
                                    InternshipStatus.APPROVED, user.getUniversity().getId()
                            ));
                } else {
                    // Все остальные (или гости) видят все одобренные стажировки
                    model.addAttribute("internships",
                            internshipRepository.findByStatus(InternshipStatus.APPROVED));
                }
            });
        } else {
            model.addAttribute("internships", internshipRepository.findByStatus(InternshipStatus.APPROVED));
        }
        return "index";
    }
}
