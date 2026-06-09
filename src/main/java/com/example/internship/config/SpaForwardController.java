package com.example.internship.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/register",
            "/student/**",
            "/company/**",
            "/university-admin/**",
            "/admin/**",
            "/verify",
            "/verify/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
