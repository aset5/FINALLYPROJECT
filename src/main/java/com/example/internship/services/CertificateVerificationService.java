package com.example.internship.services;

import com.example.internship.dto.CertificateVerificationResponse;
import com.example.internship.dto.CompletedProgramResponse;
import com.example.internship.models.Application;
import com.example.internship.models.Internship;
import com.example.internship.models.User;
import com.example.internship.repositories.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CertificateVerificationService {

    private final ApplicationRepository applicationRepository;
    private final CertificateNumberService certificateNumberService;

    public CertificateVerificationService(ApplicationRepository applicationRepository,
                                          CertificateNumberService certificateNumberService) {
        this.applicationRepository = applicationRepository;
        this.certificateNumberService = certificateNumberService;
    }

    public CertificateVerificationResponse verify(String rawNumber) {
        var parsed = certificateNumberService.parse(rawNumber);
        if (parsed.isEmpty()) {
            return CertificateVerificationResponse.invalid(
                    certificateNumberService.normalize(rawNumber != null ? rawNumber : ""),
                    "Неверный формат номера. Пример: IPRO-2026-000042");
        }

        CertificateNumberService.ParsedCertificateNumber number = parsed.get();
        Application app = applicationRepository.findByIdWithDetails(number.applicationId()).orElse(null);
        if (app == null || !CompletedProgramResponse.isCompletedUniversityProgram(app)) {
            return CertificateVerificationResponse.notFound(number.formatted());
        }

        String expected = certificateNumberService.buildNumber(app);
        if (!expected.equalsIgnoreCase(number.formatted())) {
            return CertificateVerificationResponse.notFound(number.formatted());
        }

        User student = app.getStudent();
        Internship program = app.getInternship();
        String studentName = student.getFullName() != null && !student.getFullName().isBlank()
                ? student.getFullName()
                : student.getUsername();

        LocalDate issuedAt = app.getCompletedAt() != null
                ? app.getCompletedAt().toLocalDate()
                : null;

        return new CertificateVerificationResponse(
                true,
                expected,
                studentName,
                program.getTitle(),
                program.getUniversity() != null ? program.getUniversity().getName() : null,
                app.getFinalGradePercent(),
                app.getGradeLetter(),
                issuedAt,
                "Сертификат подлинный и зарегистрирован в системе INTERN.PRO."
        );
    }
}
