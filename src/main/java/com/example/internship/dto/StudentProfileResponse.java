package com.example.internship.dto;

import java.util.List;

public record StudentProfileResponse(
        UserResponse user,
        String telegramBotUsername,
        boolean telegramConnected,
        String telegramConnectUrl,
        List<CompletedProgramResponse> completedPrograms
) {
}
