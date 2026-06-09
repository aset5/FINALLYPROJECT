package com.example.internship.dto;

import com.example.internship.models.QuizQuestion;

import java.util.List;

public record QuizQuestionResponse(
        Long id,
        int sortOrder,
        String questionText,
        List<String> options,
        Integer correctIndex
) {
    public static QuizQuestionResponse forStudent(QuizQuestion q) {
        return new QuizQuestionResponse(
                q.getId(),
                q.getSortOrder(),
                q.getQuestionText(),
                List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()),
                null
        );
    }

    public static QuizQuestionResponse forAdmin(QuizQuestion q) {
        return new QuizQuestionResponse(
                q.getId(),
                q.getSortOrder(),
                q.getQuestionText(),
                List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()),
                q.getCorrectIndex()
        );
    }
}
