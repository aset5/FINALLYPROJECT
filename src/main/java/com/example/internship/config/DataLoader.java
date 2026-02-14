package com.example.internship.config;

import com.example.internship.models.*;
import com.example.internship.repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   InternshipRepository internshipRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Создаем Админа (только если его нет)
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("1234"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                System.out.println("--- Аккаунт администратора создан (admin/admin) ---");
            }
        };
    }
}

