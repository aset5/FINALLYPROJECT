package com.example.internship.dto;

import java.util.List;

public record LearningDetailResponse(
        ApplicationResponse application,
        InternshipResponse internship,
        List<ProgramLessonResponse> lessons,
        List<ProgramMaterialResponse> materials,
        List<QuizQuestionResponse> quizQuestions,
        int progressPercent,
        boolean quizPassed,
        Integer quizScorePercent,
        boolean canComplete,
        int quizPassThreshold,
        boolean hasQuiz,
        UserResponse universityContact,
        GradeInfoResponse grades
) {}
