package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "program_materials")
public class ProgramMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "internship_id")
    private Internship internship;

    private int sortOrder;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    private MaterialType type = MaterialType.LINK;

    private String url;

    private String filePath;
}
