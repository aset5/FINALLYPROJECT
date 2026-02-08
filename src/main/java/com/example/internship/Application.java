package com.example.internship;

import com.example.internship.models.Internship;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Internship internship;

    private String studentName; // Позже заменим на полноценную модель User (Student)
    private String studentEmail;
    private String resumeUrl; // Ссылка на резюме

    private LocalDateTime appliedAt = LocalDateTime.now();
}