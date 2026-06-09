package com.example.internship.dto;

import java.util.List;

public record StudentPublicProfileResponse(
        UserResponse user,
        List<CompletedProgramResponse> completedPrograms
) {
}
