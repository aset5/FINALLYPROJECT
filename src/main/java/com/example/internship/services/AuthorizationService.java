package com.example.internship.services;

import com.example.internship.models.*;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.ProgramLessonRepository;
import com.example.internship.repositories.ProgramMaterialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Set;

@Service
public class AuthorizationService {

    public static final int MAX_COMPANY_INTERNSHIPS = 2;

    private static final Set<ApplicationStatus> COMPANY_CHAT_STATUSES = EnumSet.of(
            ApplicationStatus.ACCEPTED,
            ApplicationStatus.APPROVED
    );

    private static final Set<ApplicationStatus> ACTIVE_LEARNING_STATUSES = EnumSet.of(
            ApplicationStatus.APPROVED,
            ApplicationStatus.IN_PROGRESS,
            ApplicationStatus.COMPLETED,
            ApplicationStatus.VERIFIED
    );

    /** Одна программа ВУЗа: пока не верифицирована, новую начать нельзя */
    private static final Set<ApplicationStatus> UNIVERSITY_ENROLLMENT_ACTIVE = EnumSet.of(
            ApplicationStatus.APPROVED,
            ApplicationStatus.IN_PROGRESS,
            ApplicationStatus.COMPLETED
    );

    private final ApplicationRepository applicationRepository;
    private final InternshipRepository internshipRepository;
    private final ProgramLessonRepository lessonRepository;
    private final ProgramMaterialRepository materialRepository;
    private final StudentAchievementService studentAchievementService;

    public AuthorizationService(ApplicationRepository applicationRepository,
                                InternshipRepository internshipRepository,
                                ProgramLessonRepository lessonRepository,
                                ProgramMaterialRepository materialRepository,
                                StudentAchievementService studentAchievementService) {
        this.applicationRepository = applicationRepository;
        this.internshipRepository = internshipRepository;
        this.lessonRepository = lessonRepository;
        this.materialRepository = materialRepository;
        this.studentAchievementService = studentAchievementService;
    }

    public Internship requireOpenInternship(Long internshipId) {
        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Позиция не найдена"));
        if (internship.getStatus() != InternshipStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Позиция недоступна: не одобрена или закрыта");
        }
        return internship;
    }

    public void assertStudentCanApply(User student, Internship internship) {
        requireOpenInternship(internship.getId());

        if (internship.getUniversity() != null && internship.getCompany() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректная позиция");
        }

        if (internship.getCompany() != null) {
            if (!applicationRepository.existsByStudentIdAndStatus(student.getId(), ApplicationStatus.VERIFIED)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Отклик на вакансии доступен только верифицированным студентам");
            }
            return;
        }

        if (internship.getUniversity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Позиция недоступна для отклика");
        }

        assertSingleUniversityEnrollment(student, internship.getId());
    }

    public boolean hasActiveUniversityEnrollment(User student) {
        return findActiveUniversityApplication(student).isPresent();
    }

    public java.util.Optional<Application> findActiveUniversityApplication(User student) {
        return applicationRepository.findByStudent(student).stream()
                .filter(app -> isUniversityProgram(app.getInternship())
                        && UNIVERSITY_ENROLLMENT_ACTIVE.contains(app.getStatus()))
                .findFirst();
    }

    public void assertSingleUniversityEnrollment(User student, Long enrollingInternshipId) {
        findActiveUniversityApplication(student).ifPresent(active -> {
            if (!active.getInternship().getId().equals(enrollingInternshipId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Можно обучаться только на одной программе ВУЗа. "
                                + "Завершите текущую программу и дождитесь верификации, чтобы выбрать другую.");
            }
        });
    }

    public static boolean isUniversityProgram(Internship internship) {
        return internship.getUniversity() != null && internship.getCompany() == null;
    }

    public static boolean isCompanyJob(Internship internship) {
        return internship.getCompany() != null;
    }

    /** Сколько стажировок (ACCEPTED) у студента в компаниях */
    public long countAcceptedCompanyInternships(User student) {
        return applicationRepository.countByStudentIdAndCompanyInternshipAndStatus(
                student.getId(), ApplicationStatus.ACCEPTED);
    }

    public boolean canTakeMoreCompanyInternships(User student) {
        return countAcceptedCompanyInternships(student) < MAX_COMPANY_INTERNSHIPS;
    }

    /**
     * Лимит «прохождения» стажировок — при принятии компанией (не при отклике).
     */
    public void assertStudentCanTakeInternshipSlot(User student) {
        if (!canTakeMoreCompanyInternships(student)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "У студента уже " + MAX_COMPANY_INTERNSHIPS
                            + " активные стажировки. Принять ещё одну заявку нельзя.");
        }
    }

    public Application requireOwnUniversityApplication(Long applicationId, User admin) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        studentAchievementService.assertUniversityCanView(app, admin.getUniversity());
        return app;
    }

    public Internship requireOwnUniversityInternship(Long internshipId, User admin) {
        University university = admin.getUniversity();
        if (university == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "У пользователя не привязан университет");
        }
        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (internship.getUniversity() == null
                || !internship.getUniversity().getId().equals(university.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к этой программе");
        }
        return internship;
    }

    public void assertUniversityCanViewStudent(User admin, User student) {
        University university = admin.getUniversity();
        if (university == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        boolean linked = applicationRepository.findByStudent(student).stream()
                .anyMatch(app -> app.getInternship().getUniversity() != null
                        && app.getInternship().getUniversity().getId().equals(university.getId()));
        if (!linked) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Студент не записан на программы вашего университета");
        }
    }

    /**
     * Чат студент ↔ компания: заявка на эту вакансию в статусе ACCEPTED или APPROVED.
     */
    public void assertStudentCompanyChatAccess(User student, Internship internship) {
        if (internship.getCompany() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Чат доступен только по вакансиям компании");
        }
        boolean hasAcceptedApplication = applicationRepository.findByStudent(student).stream()
                .anyMatch(app -> app.getInternship().getId().equals(internship.getId())
                        && COMPANY_CHAT_STATUSES.contains(app.getStatus()));
        if (!hasAcceptedApplication) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Чат доступен только после принятия заявки на эту вакансию");
        }
    }

    /**
     * Получатель сообщения — только учётная запись компании, владеющей вакансией.
     * Не доверяем receiverId из тела запроса.
     */
    public User requireCompanyChatReceiver(Internship internship) {
        if (internship.getCompany() == null || internship.getCompany().getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "У вакансии не настроена компания");
        }
        return internship.getCompany().getUser();
    }

    public void assertReceiverIdMatchesCompanyUser(Long receiverId, Internship internship) {
        User companyUser = requireCompanyChatReceiver(internship);
        if (!companyUser.getId().equals(receiverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Недопустимый получатель");
        }
    }

    public void assertStudentCanDownloadLearningFile(User student, String storedName) {
        if (storedName == null || storedName.contains("..") || storedName.contains("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Long internshipId = lessonRepository.findByFilePath(storedName)
                .map(l -> l.getInternship().getId())
                .orElseGet(() -> materialRepository.findByFilePath(storedName)
                        .map(m -> m.getInternship().getId())
                        .orElse(null));

        if (internshipId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        boolean enrolled = applicationRepository.findByStudent(student).stream()
                .anyMatch(app -> app.getInternship().getId().equals(internshipId)
                        && ACTIVE_LEARNING_STATUSES.contains(app.getStatus()));
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к файлу");
        }
    }
}
