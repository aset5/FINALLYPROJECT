package com.example.internship.dto;

import java.time.LocalDate;

public record CertificateVerificationResponse(
        boolean valid,
        String certNumber,
        String studentName,
        String programTitle,
        String universityName,
        Integer finalGradePercent,
        String gradeLetter,
        LocalDate issuedAt,
        String message
) {
    public static CertificateVerificationResponse notFound(String certNumber) {
        return new CertificateVerificationResponse(
                false,
                certNumber,
                null,
                null,
                null,
                null,
                null,
                null,
                "Сертификат с таким номером не найден или программа не завершена."
        );
    }

    public static CertificateVerificationResponse invalid(String certNumber, String message) {
        return new CertificateVerificationResponse(
                false,
                certNumber,
                null,
                null,
                null,
                null,
                null,
                null,
                message
        );
    }
}
