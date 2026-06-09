package com.example.internship.dto;

import com.example.internship.models.MaterialType;
import com.example.internship.models.ProgramMaterial;

public record ProgramMaterialResponse(
        Long id,
        Long internshipId,
        int sortOrder,
        String title,
        MaterialType type,
        String url,
        String filePath,
        String fileName
) {
    public static ProgramMaterialResponse from(ProgramMaterial material) {
        String fileName = null;
        if (material.getFilePath() != null) {
            int idx = material.getFilePath().indexOf('_');
            fileName = idx >= 0 ? material.getFilePath().substring(idx + 1) : material.getFilePath();
        }
        return new ProgramMaterialResponse(
                material.getId(),
                material.getInternship().getId(),
                material.getSortOrder(),
                material.getTitle(),
                material.getType(),
                material.getUrl(),
                material.getFilePath(),
                fileName
        );
    }
}
