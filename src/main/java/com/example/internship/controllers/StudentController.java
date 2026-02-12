package com.example.internship.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/student")
public class StudentController {
    @GetMapping("/my-applications")
    public String myApps(Model model, Principal principal) {
        // Здесь будет список вакансий, на которые откликнулся студент
        return "student/applications";
    }
}
