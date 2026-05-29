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
                    "Неверный формат номера. Пример: IPRO-2026-K7M9P2XQ4R1N");
        }

        CertificateNumberService.ParsedCertificateNumber number = parsed.get();
        Application app = resolveApplication(number);
        if (app == null || !CompletedProgramResponse.isCompletedUniversityProgram(app)) {
            return CertificateVerificationResponse.notFound(number.formatted());
        }

        if (!matchesCertificateNumber(app, number)) {
            return CertificateVerificationResponse.notFound(number.formatted());
        }

        String displayNumber = number.lookupType()
                == CertificateNumberService.ParsedCertificateNumber.LookupType.TOKEN
                ? certificateNumberService.buildNumber(app)
                : number.formatted();

        return buildValidResponse(app, displayNumber);
    }

    private Application resolveApplication(CertificateNumberService.ParsedCertificateNumber number) {
        if (number.lookupType() == CertificateNumberService.ParsedCertificateNumber.LookupType.TOKEN) {
            return applicationRepository.findByCertificateTokenWithDetails(number.token()).orElse(null);
        }
        return applicationRepository.findByIdWithDetails(number.applicationId()).orElse(null);
    }

    private boolean matchesCertificateNumber(Application app,
                                             CertificateNumberService.ParsedCertificateNumber number) {
        if (number.lookupType() == CertificateNumberService.ParsedCertificateNumber.LookupType.TOKEN) {
            String expected = certificateNumberService.buildNumber(app);
            return expected.equalsIgnoreCase(number.formatted());
        }
        String legacyExpected = certificateNumberService.formatLegacy(number.year(), app.getId());
        return legacyExpected.equalsIgnoreCase(number.formatted());
    }

    private CertificateVerificationResponse buildValidResponse(Application app, String certNumber) {
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
                certNumber,
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
