package com.example.internship.dto;

import com.example.internship.models.ApplicationStatus;

import java.time.LocalDateTime;

public record AdminCertificateItemResponse(
        Long applicationId,
        String certificateNumber,
        String studentName,
        String studentUsername,
        String programTitle,
        String universityName,
        Integer finalGradePercent,
        String gradeLetter,
        LocalDateTime completedAt,
        ApplicationStatus status
) {
}
