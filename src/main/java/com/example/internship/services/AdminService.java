package com.example.internship.services;

import com.example.internship.dto.*;
import com.example.internship.models.*;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final InternshipRepository internshipRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CertificateNumberService certificateNumberService;

    public AdminService(InternshipRepository internshipRepository,
                        UserRepository userRepository,
                        ApplicationRepository applicationRepository,
                        CertificateNumberService certificateNumberService) {
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.certificateNumberService = certificateNumberService;
    }

    private static final EnumSet<Role> APPROVAL_ROLES = EnumSet.of(Role.COMPANY, Role.UNIVERSITY_ADMIN);

    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.STUDENT),
                userRepository.countByRole(Role.COMPANY),
                userRepository.countByRole(Role.UNIVERSITY_ADMIN),
                userRepository.countByEnabledFalseAndRoleIn(APPROVAL_ROLES),
                internshipRepository.countByStatus(InternshipStatus.PENDING),
                internshipRepository.countByStatus(InternshipStatus.APPROVED),
                applicationRepository.count(),
                applicationRepository.countByStatus(ApplicationStatus.COMPLETED),
                applicationRepository.countByStatus(ApplicationStatus.VERIFIED)
        );
    }

    public List<InternshipResponse> listInternships(String keyword, String status, String type) {
        InternshipStatus statusFilter = parseInternshipStatus(status);
        return internshipRepository.findAll().stream()
                .filter(i -> statusFilter == null || i.getStatus() == statusFilter)
                .filter(i -> matchesType(i, type))
                .filter(i -> matchesKeyword(i, keyword))
                .sorted(Comparator.comparing(Internship::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(InternshipResponse::from)
                .toList();
    }

    public List<InternshipResponse> listPendingInternships() {
        return internshipRepository.findByStatus(InternshipStatus.PENDING).stream()
                .map(InternshipResponse::from)
                .toList();
    }

    public List<ApplicationResponse> listApplications(String status, String keyword) {
        ApplicationStatus statusFilter = parseApplicationStatus(status);
        String q = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        return applicationRepository.findAll().stream()
                .filter(a -> statusFilter == null || a.getStatus() == statusFilter)
                .filter(a -> q.isEmpty() || matchesApplicationKeyword(a, q))
                .sorted(Comparator.comparing(
                        Application::getAppliedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ApplicationResponse::from)
                .toList();
    }

    public List<AdminCertificateItemResponse> listCertificates(String keyword) {
        String q = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        return applicationRepository.findAll().stream()
                .filter(CompletedProgramResponse::isCompletedUniversityProgram)
                .filter(a -> q.isEmpty() || matchesCertificateKeyword(a, q))
                .sorted(Comparator.comparing(
                        Application::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toCertificateItem)
                .toList();
    }

    public List<UserResponse> listUsers(String keyword, String role, Boolean pendingOnly) {
        Role roleFilter = parseRole(role);
        String q = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        return userRepository.findAll().stream()
                .filter(u -> roleFilter == null || u.getRole() == roleFilter)
                .filter(u -> pendingOnly == null || !pendingOnly
                        || (!u.isEnabled() && APPROVAL_ROLES.contains(u.getRole())))
                .filter(u -> q.isEmpty() || matchesUserKeyword(u, q))
                .sorted(Comparator.comparing(User::getId))
                .map(UserResponse::from)
                .toList();
    }

    public List<UserResponse> listPendingAccountApprovals() {
        return userRepository.findByEnabledFalseAndRoleInOrderByIdAsc(APPROVAL_ROLES).stream()
                .map(UserResponse::from)
                .toList();
    }

    private AdminCertificateItemResponse toCertificateItem(Application app) {
        User student = app.getStudent();
        Internship program = app.getInternship();
        String name = student.getFullName() != null && !student.getFullName().isBlank()
                ? student.getFullName()
                : student.getUsername();
        return new AdminCertificateItemResponse(
                app.getId(),
                certificateNumberService.buildNumber(app),
                name,
                student.getUsername(),
                program.getTitle(),
                program.getUniversity() != null ? program.getUniversity().getName() : null,
                app.getFinalGradePercent(),
                app.getGradeLetter(),
                app.getCompletedAt(),
                app.getStatus()
        );
    }

    private static boolean matchesType(Internship i, String type) {
        if (type == null || type.isBlank() || "all".equalsIgnoreCase(type)) {
            return true;
        }
        if ("company".equalsIgnoreCase(type)) {
            return i.getCompany() != null;
        }
        if ("university".equalsIgnoreCase(type)) {
            return i.getUniversity() != null;
        }
        return true;
    }

    private static boolean matchesKeyword(Internship i, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String q = keyword.toLowerCase(Locale.ROOT);
        return contains(i.getTitle(), q)
                || contains(i.getCity(), q)
                || contains(i.getDescription(), q)
                || (i.getUniversity() != null && contains(i.getUniversity().getName(), q))
                || (i.getCompany() != null && contains(i.getCompany().getName(), q));
    }

    private static boolean matchesApplicationKeyword(Application a, String q) {
        User s = a.getStudent();
        Internship i = a.getInternship();
        return contains(s != null ? s.getUsername() : null, q)
                || contains(s != null ? s.getFullName() : null, q)
                || contains(i != null ? i.getTitle() : null, q)
                || (i != null && i.getUniversity() != null && contains(i.getUniversity().getName(), q))
                || (i != null && i.getCompany() != null && contains(i.getCompany().getName(), q));
    }

    private static boolean matchesCertificateKeyword(Application a, String q) {
        return matchesApplicationKeyword(a, q);
    }

    private static boolean matchesUserKeyword(User u, String q) {
        return contains(u.getUsername(), q)
                || contains(u.getFullName(), q)
                || contains(u.getEmail(), q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private static InternshipStatus parseInternshipStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return InternshipStatus.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ApplicationStatus parseApplicationStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ApplicationStatus.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Role parseRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
