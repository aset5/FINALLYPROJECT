package com.example.internship.dto;

import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;

public record InternshipResponse(
        Long id,
        String title,
        String city,
        String description,
        String studyMaterials,
        InternshipStatus status,
        int maxPlaces,
        int joinedCount,
        Long universityId,
        String universityName,
        Long companyId,
        String companyName,
        boolean companyJob
) {
    public static InternshipResponse from(Internship internship) {
        Long universityId = internship.getUniversity() != null ? internship.getUniversity().getId() : null;
        String universityName = internship.getUniversity() != null ? internship.getUniversity().getName() : null;
        Long companyId = internship.getCompany() != null ? internship.getCompany().getId() : null;
        String companyName = internship.getCompany() != null ? internship.getCompany().getName() : null;
        return new InternshipResponse(
                internship.getId(),
                internship.getTitle(),
                internship.getCity(),
                internship.getDescription(),
                internship.getStudyMaterials(),
                internship.getStatus(),
                internship.getMaxPlaces(),
                internship.getJoinedCount(),
                universityId,
                universityName,
                companyId,
                companyName,
                internship.getCompany() != null
        );
    }
}
