package com.example.internship.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String roleType,
        Long universityId,
        String uniName,
        String fullName,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Код должен состоять из 6 цифр") String verificationCode
) {
}
