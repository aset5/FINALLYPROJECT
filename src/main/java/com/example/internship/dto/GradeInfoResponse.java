package com.example.internship.dto;

public record GradeInfoResponse(
        int moduleAveragePercent,
        Integer finalTestPercent,
        int overallGradePercent,
        String gradeLetter,
        int modulesWeightPercent,
        int finalTestWeightPercent,
        int minPassPercent,
        boolean gradeRequirementMet
) {}
