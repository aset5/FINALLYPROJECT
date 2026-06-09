package com.example.internship.dto;

import com.example.internship.models.Application;
import com.example.internship.models.ApplicationStatus;

import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        UserResponse student,
        InternshipResponse internship,
        Integer finalGradePercent,
        String gradeLetter,
        LocalDateTime completedAt
) {
    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getStatus(),
                application.getAppliedAt(),
                UserResponse.from(application.getStudent()),
                InternshipResponse.from(application.getInternship()),
                application.getFinalGradePercent(),
                application.getGradeLetter(),
                application.getCompletedAt()
        );
    }
}
