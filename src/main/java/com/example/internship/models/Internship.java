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

    private boolean isJob = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private InternshipStatus status = InternshipStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "university_id")
    private University university;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @OneToMany(mappedBy = "internship", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Application> applications;

    private int maxPlaces = 0;

    @Column(name = "joined_count")
    private int joinedCount = 0;

    public boolean hasAvailablePlaces() {
        return joinedCount < maxPlaces;
    }
}