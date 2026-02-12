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
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                System.out.println("--- Аккаунт администратора создан (admin/admin) ---");
            }

            // 2. Создаем вакансии ТОЛЬКО если таблица пуста
            if (internshipRepository.count() == 0) {
                Internship i1 = new Internship();
                i1.setTitle("Java Developer Intern");
                i1.setCity("Almaty");
                i1.setDescription("Learn Spring Boot and Hibernate.");
                i1.setStatus(InternshipStatus.APPROVED);

                Internship i2 = new Internship();
                i2.setTitle("Frontend Developer");
                i2.setCity("Astana");
                i2.setDescription("Work with React and Tailwind.");
                i2.setStatus(InternshipStatus.APPROVED);

                Internship i3 = new Internship();
                i3.setTitle("Python Data Science");
                i3.setCity("Remote");
                i3.setDescription("Secret internship project.");
                i3.setStatus(InternshipStatus.PENDING);

                internshipRepository.save(i1);
                internshipRepository.save(i2);
                internshipRepository.save(i3);

                System.out.println("--- Начальные вакансии загружены! ---");
            } else {
                System.out.println("--- Вакансии уже есть в базе, пропускаем инициализацию ---");
            }
        };
    }
}