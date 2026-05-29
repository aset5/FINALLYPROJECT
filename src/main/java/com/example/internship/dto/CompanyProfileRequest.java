package com.example.internship.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyProfileRequest(
        @NotBlank String name,
        String bin
) {
}
