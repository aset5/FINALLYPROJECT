package com.example.internship.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "internship_id")
    private Internship internship;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student; // Пользователь с ролью STUDENT

    private LocalDateTime appliedAt = LocalDateTime.now();
}