package com.example.internship.dto;

import com.example.internship.models.Application;
import com.example.internship.models.ApplicationStatus;
import com.example.internship.models.Internship;
import com.example.internship.models.University;

import java.time.LocalDateTime;

public record CompletedProgramResponse(
        Long applicationId,
        String certificateNumber,
        String programTitle,
        String universityName,
        Integer finalGradePercent,
        String gradeLetter,
        LocalDateTime completedAt
) {
    public static CompletedProgramResponse from(Application application, String certificateNumber) {
        Internship internship = application.getInternship();
        University university = internship != null ? internship.getUniversity() : null;
        return new CompletedProgramResponse(
                application.getId(),
                certificateNumber,
                internship != null ? internship.getTitle() : "Программа",
                university != null ? university.getName() : null,
                application.getFinalGradePercent(),
                application.getGradeLetter(),
                application.getCompletedAt()
        );
    }

    /** Завершённая программа ВУЗа: COMPLETED или VERIFIED (после верификации куратором). */
    public static boolean isCompletedUniversityProgram(Application application) {
        ApplicationStatus status = application.getStatus();
        if (status != ApplicationStatus.COMPLETED && status != ApplicationStatus.VERIFIED) {
            return false;
        }
        if (application.getCompletedAt() == null) {
            return false;
        }
        Internship internship = application.getInternship();
        return internship != null && internship.getUniversity() != null;
    }
}
