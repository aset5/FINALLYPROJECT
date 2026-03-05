package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "internships")
public class Internship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String city;

    @Column(columnDefinition = "TEXT")
    private String studyMaterials;
    // В модели Internship.java
    private boolean isJob = false; // true - если это вакансия компании, false - если стажировка вуза
    @Column(columnDefinition = "TEXT")
    private String description;

    // Оставляем один статус. По умолчанию — PENDING.
    @Enumerated(EnumType.STRING)
    private InternshipStatus status = InternshipStatus.PENDING;

    // Связь с Университетом (кто курирует/создал сейчас)
    @ManyToOne
    @JoinColumn(name = "university_id")
    private University university;

    // Связь с Компанией (оставляем для будущего трудоустройства)
    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    // Список заявок студентов
    @OneToMany(mappedBy = "internship", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Application> applications;

    private int maxPlaces = 0;

    public boolean hasAvailablePlaces() {
        return applications == null || applications.size() < maxPlaces;
    }
}