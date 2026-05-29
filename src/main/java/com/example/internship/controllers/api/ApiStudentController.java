package com.example.internship.controllers.api;

import com.example.internship.dto.*;
import com.example.internship.models.*;
import com.example.internship.repositories.*;
import com.example.internship.services.AuthorizationService;
import com.example.internship.services.StudentAchievementService;
import com.example.internship.services.TelegramNotificationSender;
import org.springframework.web.server.ResponseStatusException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class ApiStudentController {

    private static final String UPLOAD_DIR = "uploads/resumes/";

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InternshipRepository internshipRepository;
    private final MessageRepository messageRepository;
    private final TelegramNotificationSender telegramNotifications;
    private final StudentAchievementService studentAchievementService;
    private final AuthorizationService authorizationService;

    @Value("${telegram.bot.name}")
    private String telegramBotUsername;

    public ApiStudentController(ApplicationRepository applicationRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                InternshipRepository internshipRepository,
                                MessageRepository messageRepository,
                                TelegramNotificationSender telegramNotifications,
                                StudentAchievementService studentAchievementService,
                                AuthorizationService authorizationService) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.internshipRepository = internshipRepository;
        this.messageRepository = messageRepository;
        this.telegramNotifications = telegramNotifications;
        this.studentAchievementService = studentAchievementService;
        this.authorizationService = authorizationService;
    }

    private User currentUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername()).orElseThrow();
    }

    private StudentProfileResponse buildProfileResponse(User user) {
        boolean connected = user.getTelegramChatId() != null;
        String connectUrl = "https://t.me/" + telegramBotUsername + "?start=user_" + user.getId();
        return new StudentProfileResponse(
                UserResponse.from(user),
                telegramBotUsername,
                connected,
                connectUrl,
                studentAchievementService.completedProgramsForStudent(user)
        );
    }

    @GetMapping("/applications")
    public Map<String, Object> applications(@AuthenticationPrincipal UserDetails principal) {
        User student = currentUser(principal);
        List<Application> apps = applicationRepository.findByStudent(student);
        boolean isVerified = apps.stream()
                .anyMatch(app -> app.getStatus() == ApplicationStatus.VERIFIED);

        Set<Long> appliedInternshipIds = apps.stream()
                .map(a -> a.getInternship().getId())
                .collect(Collectors.toSet());

        List<InternshipResponse> companyJobs = List.of();
        if (isVerified) {
            companyJobs = internshipRepository.findAll().stream()
                    .filter(i -> i.getCompany() != null)
                    .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                    .map(InternshipResponse::from)
                    .toList();
        }

        return Map.of(
                "user", UserResponse.from(student),
                "isVerified", isVerified,
                "hasActiveUniversityProgram", authorizationService.hasActiveUniversityEnrollment(student),
                "activeUniversityApplicationId",
                authorizationService.findActiveUniversityApplication(student)
                        .map(Application::getId)
                        .orElse(null),
                "appliedInternshipIds", appliedInternshipIds,
                "acceptedCompanyInternships", authorizationService.countAcceptedCompanyInternships(student),
                "maxCompanyInternships", AuthorizationService.MAX_COMPANY_INTERNSHIPS,
                "canTakeMoreInternships", authorizationService.canTakeMoreCompanyInternships(student),
                "applications", apps.stream().map(ApplicationResponse::from).toList(),
                "companyJobs", companyJobs
        );
    }

    @PostMapping("/apply/{id}")
    @Transactional
    public ResponseEntity<?> apply(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails principal) {
        User student = currentUser(principal);
        Internship internship = authorizationService.requireOpenInternship(id);
        authorizationService.assertStudentCanApply(student, internship);

        if (applicationRepository.existsByStudentAndInternship(student, internship)) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("Вы уже подали заявку на эту позицию"));
        }

        Application application = new Application();
        application.setStudent(student);
        application.setInternship(internship);
        application.setAppliedAt(LocalDateTime.now());

        if (internship.getUniversity() != null) {
            if (internship.getMaxPlaces() > 0 && internship.getJoinedCount() >= internship.getMaxPlaces()) {
                return ResponseEntity.badRequest().body(new ApiError("Свободных мест не осталось"));
            }
            application.setStatus(ApplicationStatus.APPROVED);
            internship.setJoinedCount(internship.getJoinedCount() + 1);
            internshipRepository.save(internship);
        } else {
            application.setStatus(ApplicationStatus.PENDING);
        }

        applicationRepository.save(application);
        return ResponseEntity.ok(ApplicationResponse.from(application));
    }

    @GetMapping("/job-market")
    public ResponseEntity<?> jobMarket(@AuthenticationPrincipal UserDetails principal) {
        User student = currentUser(principal);
        boolean isVerified = applicationRepository.existsByStudentIdAndStatus(
                student.getId(), ApplicationStatus.VERIFIED);
        if (!isVerified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("Доступ только для верифицированных студентов"));
        }
        List<Application> myApps = applicationRepository.findByStudent(student);
        Set<Long> appliedInternshipIds = myApps.stream()
                .map(a -> a.getInternship().getId())
                .collect(Collectors.toSet());

        List<InternshipResponse> jobs = internshipRepository.findAll().stream()
                .filter(i -> i.getCompany() != null)
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .map(InternshipResponse::from)
                .toList();
        return ResponseEntity.ok(Map.of(
                "jobs", jobs,
                "appliedInternshipIds", appliedInternshipIds,
                "acceptedCompanyInternships", authorizationService.countAcceptedCompanyInternships(student),
                "maxCompanyInternships", AuthorizationService.MAX_COMPANY_INTERNSHIPS,
                "canTakeMoreInternships", authorizationService.canTakeMoreCompanyInternships(student)
        ));
    }

    @GetMapping("/profile")
    public StudentProfileResponse profile(@AuthenticationPrincipal UserDetails principal) {
        return buildProfileResponse(currentUser(principal));
    }

    @PutMapping("/profile")
    public StudentProfileResponse updateProfile(@RequestParam(required = false) String fullName,
                                                @RequestParam(required = false) String email,
                                                @RequestParam(required = false) String resume,
                                                @RequestParam(value = "resumeFile", required = false) MultipartFile file,
                                                @AuthenticationPrincipal UserDetails principal) throws IOException {
        User user = currentUser(principal);
        if (fullName != null) user.setFullName(fullName);
        if (email != null) user.setEmail(email);
        if (resume != null) user.setResume(resume);

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            user.setResumePath(fileName);
        }

        userRepository.save(user);
        return buildProfileResponse(user);
    }

    @PostMapping("/profile/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(new ApiError("Қазіргі пароль қате!"));
        }
        String passwordPattern = "^(?=.*[A-Z]).{8,}$";
        if (!newPassword.matches(passwordPattern)) {
            return ResponseEntity.badRequest().body(new ApiError(
                    "Пароль кемінде 8 символ және бір үлкен әріптен тұруы керек!"));
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(new ApiError("Парольдер сәйкес емес!"));
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Пароль сәтті өзгертілді!"));
    }

    @GetMapping("/messages/{internshipId}")
    public ResponseEntity<?> chat(@PathVariable Long internshipId,
                                  @AuthenticationPrincipal UserDetails principal) {
        User me = currentUser(principal);
        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        authorizationService.assertStudentCompanyChatAccess(me, internship);
        User companyUser = authorizationService.requireCompanyChatReceiver(internship);

        List<MessageResponse> history = messageRepository
                .findByInternshipIdAndSenderIdAndReceiverIdOrInternshipIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                        internshipId, me.getId(), companyUser.getId(),
                        internshipId, companyUser.getId(), me.getId())
                .stream()
                .map(MessageResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "history", history,
                "companyUser", UserResponse.from(companyUser),
                "internshipId", internshipId,
                "internshipTitle", internship.getTitle(),
                "currentUsername", me.getUsername()
        ));
    }

    @PostMapping("/messages")
    public MessageResponse sendMessage(@RequestBody Map<String, Object> body,
                                       @AuthenticationPrincipal UserDetails principal) {
        User me = currentUser(principal);
        if (body.get("internshipId") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите internshipId");
        }
        Long internshipId = Long.valueOf(body.get("internshipId").toString());
        String content = body.get("content") != null ? body.get("content").toString().trim() : "";
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пустое сообщение");
        }

        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Вакансия не найдена"));

        // 1) заявка студента на эту вакансию; 2) статус ACCEPTED или APPROVED
        authorizationService.assertStudentCompanyChatAccess(me, internship);

        // 3) получатель — только user компании этой вакансии (не из произвольного receiverId)
        User receiver = authorizationService.requireCompanyChatReceiver(internship);
        if (body.get("receiverId") != null) {
            Long claimedReceiverId = Long.valueOf(body.get("receiverId").toString());
            authorizationService.assertReceiverIdMatchesCompanyUser(claimedReceiverId, internship);
        }

        Message msg = new Message();
        msg.setSender(me);
        msg.setReceiver(receiver);
        msg.setContent(content);
        msg.setInternship(internship);
        msg.setSentAt(LocalDateTime.now());
        messageRepository.save(msg);

        if (receiver.getTelegramChatId() != null) {
            telegramNotifications.sendNotification(receiver.getTelegramChatId(),
                    "🎓 Новый вопрос от студента " + me.getUsername() +
                            " по вакансии \"" + internship.getTitle() + "\":\n\n" + content);
        }

        return MessageResponse.from(msg);
    }

}
