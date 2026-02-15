package com.example.internship.models;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public enum InternshipStatus {

    PENDING, APPROVED, REJECTED;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.PENDING;
}
