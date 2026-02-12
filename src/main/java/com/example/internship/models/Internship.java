package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Internship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String city;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private InternshipStatus status = InternshipStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company; // Кто разместил вакансию

    // Геттеры и сеттеры
}
