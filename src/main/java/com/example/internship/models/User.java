package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String resume;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role; // STUDENT, COMPANY, ADMIN

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Company company;

    public String getResume() { return resume; }
    public void setResume(String resume) { this.resume = resume; }

    // Геттеры и сеттеры

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;
}