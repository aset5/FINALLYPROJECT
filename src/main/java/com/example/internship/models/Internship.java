package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Internship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "internship", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Application> applications;

    private String title;
    private String city;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Оставляем только этот статус!
    @Enumerated(EnumType.STRING)
    private InternshipStatus status = InternshipStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}