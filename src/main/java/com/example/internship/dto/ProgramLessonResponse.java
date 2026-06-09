package com.example.internship.dto;

import com.example.internship.models.LessonProgress;
import com.example.internship.models.ProgramLesson;

import java.util.List;

public record ProgramLessonResponse(
        Long id,
        Long internshipId,
        int sortOrder,
        String title,
        String content,
        String externalUrl,
        String filePath,
        String fileName,
        boolean completed,
        Integer scorePercent,
        boolean hasCheckQuestion,
        String checkQuestion,
        List<String> checkOptions,
        Integer checkCorrectIndex
) {
    public static ProgramLessonResponse from(ProgramLesson lesson, LessonProgress progress, boolean forAdmin) {
        String fileName = null;
        if (lesson.getFilePath() != null) {
            int idx = lesson.getFilePath().indexOf('_');
            fileName = idx >= 0 ? lesson.getFilePath().substring(idx + 1) : lesson.getFilePath();
        }
        boolean hasCheck = lesson.hasCheckQuestion();
        List<String> options = hasCheck
                ? List.of(lesson.getCheckOptionA(), lesson.getCheckOptionB(),
                lesson.getCheckOptionC(), lesson.getCheckOptionD())
                : List.of();
        return new ProgramLessonResponse(
                lesson.getId(),
                lesson.getInternship().getId(),
                lesson.getSortOrder(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getExternalUrl(),
                lesson.getFilePath(),
                fileName,
                progress != null,
                progress != null ? progress.getScorePercent() : null,
                hasCheck,
                hasCheck ? lesson.getCheckQuestion() : null,
                forAdmin ? options : (hasCheck ? options : List.of()),
                forAdmin && hasCheck ? lesson.getCheckCorrectIndex() : null
        );
    }
}
