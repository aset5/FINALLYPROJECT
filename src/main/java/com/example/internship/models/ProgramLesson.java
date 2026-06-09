package com.example.internship.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "program_lessons")
public class ProgramLesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "internship_id")
    private Internship internship;

    private int sortOrder;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String externalUrl;

    private String filePath;

    /** Контрольный вопрос по модулю (необязательно) */
    @Column(columnDefinition = "TEXT")
    private String checkQuestion;

    private String checkOptionA;
    private String checkOptionB;
    private String checkOptionC;
    private String checkOptionD;

    /** 0–3, индекс правильного варианта */
    private Integer checkCorrectIndex;

    public boolean hasCheckQuestion() {
        return checkQuestion != null && !checkQuestion.isBlank()
                && checkCorrectIndex != null
                && checkOptionA != null && !checkOptionA.isBlank()
                && checkOptionB != null && !checkOptionB.isBlank();
    }
}
