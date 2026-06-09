package com.example.internship.dto;

import com.example.internship.models.Role;
import com.example.internship.models.User;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        String resume,
        String resumePath,
        Role role,
        Long universityId,
        String universityName,
        Long telegramChatId,
        boolean enabled
) {
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        Long universityId = user.getUniversity() != null ? user.getUniversity().getId() : null;
        String universityName = user.getUniversity() != null ? user.getUniversity().getName() : null;
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getResume(),
                user.getResumePath(),
                user.getRole(),
                universityId,
                universityName,
                user.getTelegramChatId(),
                user.isEnabled()
        );
    }
}
