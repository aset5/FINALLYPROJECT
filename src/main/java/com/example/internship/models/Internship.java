package com.example.internship.models;

import com.example.internship.models.User;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Internship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String city;

    @Enumerated(EnumType.STRING) // Это сохранит в БД строку "APPROVED" или "PENDING"
    private InternshipStatus status = InternshipStatus.PENDING;
    @ManyToOne
    private Company company;
}