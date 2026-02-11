package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class InternshipApplicationModel { // Изменили имя здесь
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User student;

    @ManyToOne
    private Internship internship;

    private LocalDateTime applyDate = LocalDateTime.now();
    private String status = "PENDING";
}