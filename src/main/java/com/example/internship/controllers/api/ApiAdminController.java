package com.example.internship.controllers.api;

import com.example.internship.dto.*;
import com.example.internship.models.*;
import com.example.internship.repositories.*;
import com.example.internship.services.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

    private final InternshipRepository internshipRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final MessageRepository messageRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminService adminService;

    public ApiAdminController(InternshipRepository internshipRepository,
                              UserRepository userRepository,
                              ApplicationRepository applicationRepository,
                              CompanyRepository companyRepository,
                              MessageRepository messageRepository,
                              LessonProgressRepository lessonProgressRepository,
                              PasswordEncoder passwordEncoder,
                              AdminService adminService) {
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.messageRepository = messageRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.getStats();
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return Map.of(
                "stats", adminService.getStats(),
                "pendingAccountApprovals", adminService.listPendingAccountApprovals(),
                "pendingInternships", adminService.listPendingInternships(),
                "internships", adminService.listInternships(keyword, status, type)
        );
    }

    @GetMapping("/applications")
    public Map<String, Object> applications(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Map.of("applications", adminService.listApplications(status, keyword));
    }

    @GetMapping("/certificates")
    public Map<String, Object> certificates(@RequestParam(required = false) String keyword) {
        return Map.of("certificates", adminService.listCertificates(keyword));
    }

    @GetMapping("/users")
    public Map<String, Object> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean pendingOnly) {
        return Map.of("users", adminService.listUsers(keyword, role, pendingOnly));
    }

    @PostMapping("/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        if (user.getRole() != Role.COMPANY && user.getRole() != Role.UNIVERSITY_ADMIN) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("Активация доступна только для компаний и представителей ВУЗа"));
        }
        if (user.isEnabled()) {
            return ResponseEntity.ok(UserResponse.from(user));
        }
        user.setEnabled(true);
        return ResponseEntity.ok(UserResponse.from(userRepository.save(user)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(id).orElseThrow();
        String newUsername = body.get("username") != null ? body.get("username").toString() : user.getUsername();
        String newPassword = body.get("password") != null ? body.get("password").toString() : null;

        if (!user.getUsername().equals(newUsername) && userRepository.existsByUsername(newUsername)) {
            return ResponseEntity.badRequest().body(new ApiError("Логин уже занят"));
        }
        user.setUsername(newUsername);

        if (body.containsKey("enabled")) {
            Object enabledVal = body.get("enabled");
            boolean enabled = enabledVal instanceof Boolean
                    ? (Boolean) enabledVal
                    : Boolean.parseBoolean(String.valueOf(enabledVal));
            if (user.getRole() == Role.ADMIN && !enabled) {
                return ResponseEntity.badRequest()
                        .body(new ApiError("Нельзя заблокировать учётную запись администратора"));
            }
            user.setEnabled(enabled);
        }

        if (newPassword != null && !newPassword.isBlank()) {
            String passwordPattern = "^(?=.*[A-Z]).{8,}$";
            if (!newPassword.matches(passwordPattern)) {
                return ResponseEntity.badRequest().body(new ApiError(
                        "Пароль должен быть не менее 8 символов и содержать заглавную букву"));
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        userRepository.save(user);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @Transactional
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        messageRepository.deleteBySenderIdOrReceiverId(id, id);
        lessonProgressRepository.deleteByApplication_Student_Id(user.getId());
        applicationRepository.deleteByStudent(user);

        Company company = companyRepository.findByUserId(user.getId());
        if (company != null) {
            List<Internship> companyJobs = internshipRepository.findByCompany(company);
            for (Internship job : companyJobs) {
                messageRepository.deleteByInternship(job);
                lessonProgressRepository.deleteByApplication_Internship_Id(job.getId());
                applicationRepository.deleteByInternship(job);
            }
            internshipRepository.deleteByCompany(company);
            companyRepository.delete(company);
        }
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internships/{id}/approve")
    public InternshipResponse approve(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        internship.setStatus(InternshipStatus.APPROVED);
        return InternshipResponse.from(internshipRepository.save(internship));
    }

    @PostMapping("/internships/{id}/reject")
    public InternshipResponse reject(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        internship.setStatus(InternshipStatus.REJECTED);
        return InternshipResponse.from(internshipRepository.save(internship));
    }

    @Transactional
    @DeleteMapping("/internships/{id}")
    public ResponseEntity<Void> deleteInternship(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        messageRepository.deleteByInternship(internship);
        lessonProgressRepository.deleteByApplication_Internship_Id(internship.getId());
        applicationRepository.deleteByInternship(internship);
        internshipRepository.delete(internship);
        return ResponseEntity.noContent().build();
    }
}
