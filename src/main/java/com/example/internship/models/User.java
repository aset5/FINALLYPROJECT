package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // Добавляем отсутствующие поля:
    private String fullName;
    private String email;
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String resume; // Текстовое описание

    private String resumePath; // Путь к PDF файлу

    @Enumerated(EnumType.STRING)
    private Role role; // STUDENT, COMPANY, ADMIN

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Company company;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    // С аннотацией @Data от Lombok геттеры и сеттеры создаются автоматически.
    // Если Lombok не подхватывается, оставь ручные методы ниже:

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getResume() { return resume; }
    public void setResume(String resume) { this.resume = resume; }
}