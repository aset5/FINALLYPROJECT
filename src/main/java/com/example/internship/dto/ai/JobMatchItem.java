package com.example.internship.dto.ai;

import java.util.List;

public record JobMatchItem(
        Long internshipId,
        String title,
        String companyName,
        String city,
        int matchPercent,
        String summary,
        List<String> skillsToImprove
) {
}
