package com.example.internship.dto.ai;

public record JobMatchStats(
        int totalJobs,
        int analyzedJobs,
        double averageMatchPercent,
        int highMatchCount,
        Long bestMatchInternshipId,
        int bestMatchPercent
) {
}
