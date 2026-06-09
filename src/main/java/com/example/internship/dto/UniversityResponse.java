package com.example.internship.dto;

import com.example.internship.models.University;

public record UniversityResponse(Long id, String name) {
    public static UniversityResponse from(University university) {
        if (university == null) {
            return null;
        }
        return new UniversityResponse(university.getId(), university.getName());
    }
}
