package com.example.internship.controllers;

import com.example.internship.models.*;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MainController {
    @Autowired
    private InternshipRepository internshipRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApplicationRepository applicationRepository;


    @GetMapping("/")
    public String index(Model model, Principal principal) {
        User user = null;
        boolean isVerified = false;
        Long studentUniId = null;

        if (principal != null) {
            user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                isVerified = applicationRepository.existsByStudentIdAndStatus(user.getId(), ApplicationStatus.VERIFIED);
                if (user.getUniversity() != null) {
                    studentUniId = user.getUniversity().getId();
                }
            }
        }


        final Long finalStudentUniId = studentUniId;
        List<Internship> universityPrograms = internshipRepository.findAll().stream()
                .filter(i -> i.getUniversity() != null)
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .filter(i -> finalStudentUniId != null && i.getUniversity().getId().equals(finalStudentUniId))
                .collect(Collectors.toList());

        List<Internship> companyJobs = internshipRepository.findAll().stream()
                .filter(i -> i.getCompany() != null)
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .collect(Collectors.toList());

        model.addAttribute("uniPrograms", universityPrograms);
        model.addAttribute("companyJobs", companyJobs);
        model.addAttribute("isVerified", isVerified);

        return "index";
    }
}