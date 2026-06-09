package com.example.internship.services;

import com.example.internship.dto.CompletedProgramResponse;
import com.example.internship.models.Application;
import com.example.internship.models.Company;
import com.example.internship.models.Internship;
import com.example.internship.models.University;
import com.example.internship.models.User;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.CompanyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class StudentAchievementService {

    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final CertificateNumberService certificateNumberService;

    public StudentAchievementService(ApplicationRepository applicationRepository,
                                   CompanyRepository companyRepository,
                                   CertificateNumberService certificateNumberService) {
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.certificateNumberService = certificateNumberService;
    }

    public List<CompletedProgramResponse> completedProgramsForStudent(User student) {
        return applicationRepository.findByStudent(student).stream()
                .filter(CompletedProgramResponse::isCompletedUniversityProgram)
                .sorted(byCompletedAtDesc())
                .map(this::toCompletedProgram)
                .toList();
    }

    public List<CompletedProgramResponse> completedProgramsForStudentAtUniversity(
            User student,
            University university) {
        if (university == null) {
            return List.of();
        }
        return applicationRepository.findByStudent(student).stream()
                .filter(CompletedProgramResponse::isCompletedUniversityProgram)
                .filter(app -> {
                    Internship internship = app.getInternship();
                    return internship.getUniversity() != null
                            && internship.getUniversity().getId().equals(university.getId());
                })
                .sorted(byCompletedAtDesc())
                .map(this::toCompletedProgram)
                .toList();
    }

    private CompletedProgramResponse toCompletedProgram(Application application) {
        return CompletedProgramResponse.from(
                application,
                certificateNumberService.buildNumber(application));
    }

    private static Comparator<Application> byCompletedAtDesc() {
        return Comparator.comparing(
                Application::getCompletedAt,
                Comparator.nullsLast(Comparator.reverseOrder()));
    }

    public Application requireCompletedProgram(Long applicationId) {
        Application app = applicationRepository.findByIdWithDetails(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка не найдена"));
        if (!CompletedProgramResponse.isCompletedUniversityProgram(app)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Программа не завершена");
        }
        return app;
    }

    public void assertStudentOwnsApplication(Application app, User student) {
        if (!app.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public void assertUniversityCanView(Application app, University university) {
        if (university == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Internship internship = app.getInternship();
        if (internship.getUniversity() == null
                || !internship.getUniversity().getId().equals(university.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к этой программе");
        }
    }

    public void assertCompanyCanViewStudent(User companyUser, User student) {
        Company company = companyRepository.findByUserId(companyUser.getId());
        if (company == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Профиль компании не настроен");
        }
        boolean linked = applicationRepository.findByInternshipCompanyId(company.getId()).stream()
                .anyMatch(app -> app.getStudent().getId().equals(student.getId()));
        if (!linked) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Сертификат доступен только для кандидатов ваших вакансий");
        }
    }
}
