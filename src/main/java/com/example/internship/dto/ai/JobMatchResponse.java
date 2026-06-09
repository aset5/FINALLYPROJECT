package com.example.internship.dto.ai;

import java.util.List;

public record JobMatchResponse(
        List<JobMatchItem> matches,
        String overallAdvice,
        JobMatchStats stats
) {
}
