package com.example.internship.services;

import com.example.internship.models.*;
import com.example.internship.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepo;
    @Autowired private InternshipRepository internRepo;
    @Autowired private CompanyRepository companyRepo; // Добавь этот репозиторий
    @Autowired private PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (userRepo.count() == 0) {
            // 1. Создаем Админа
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("123"));
            admin.setRole("ROLE_ADMIN");
            userRepo.save(admin);

            // 2. Создаем Студента
            User student = new User();
            student.setUsername("student");
            student.setPassword(encoder.encode("123"));
            student.setRole("ROLE_STUDENT");
            userRepo.save(student);

            // 3. Создаем Аккаунт для Компании
            User companyUser = new User();
            companyUser.setUsername("kaspi_user");
            companyUser.setPassword(encoder.encode("123"));
            companyUser.setRole("ROLE_COMPANY");
            userRepo.save(companyUser);

            // 4. Создаем профиль Компании (Сущность Company)
            Company kaspi = new Company();
            kaspi.setName("Kaspi.kz");
            kaspi.setBin("123456789012");
            // Если в твоей модели Company есть связь с User, добавь: kaspi.setUser(companyUser);
            companyRepo.save(kaspi);

            // 5. Создаем тестовую вакансию
            Internship job = new Internship();
            job.setTitle("Java Developer Intern (Kaspi)");
            job.setDescription("Обучение разработке высоконагруженных систем на Java и Spring Boot.");
            job.setCity("Алматы");
            job.setStatus(InternshipStatus.APPROVED);

            // ТЕПЕРЬ ПРАВИЛЬНО: передаем объект Company, а не User
            job.setCompany(kaspi);

            internRepo.save(job);

            System.out.println(">> Данные для диплома успешно инициализированы!");
        }
    }
}