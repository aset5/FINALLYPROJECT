package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "lesson_id"})
)
public class LessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "application_id")
    private Application application;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lesson_id")
    private ProgramLesson lesson;

    private LocalDateTime completedAt = LocalDateTime.now();

    /** Балл за модуль 0–100 */
    @Column(name = "score_percent", nullable = false)
    private int scorePercent = 100;

    @Column(nullable = false)
    private int attempts = 1;
}
