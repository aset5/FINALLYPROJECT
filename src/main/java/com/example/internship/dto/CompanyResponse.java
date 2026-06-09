package com.example.internship.dto;

import com.example.internship.models.Company;

public record CompanyResponse(Long id, String name, String bin, Long userId) {
    public static CompanyResponse from(Company company) {
        if (company == null) {
            return null;
        }
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getBin(),
                company.getUser() != null ? company.getUser().getId() : null
        );
    }
}
