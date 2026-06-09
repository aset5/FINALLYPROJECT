package com.example.internship.controllers.api;

import com.example.internship.dto.*;
import com.example.internship.models.*;
import com.example.internship.repositories.*;
import com.example.internship.services.StudentAchievementService;
import com.example.internship.services.TelegramBotService;
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
    private final TelegramBotService telegramBotService;
    private final StudentAchievementService studentAchievementService;

    @Value("${telegram.bot.name}")
    private String telegramBotUsername;

    public ApiStudentController(ApplicationRepository applicationRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                InternshipRepository internshipRepository,
                                MessageRepository messageRepository,
                                TelegramBotService telegramBotService,
                                StudentAchievementService studentAchievementService) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.internshipRepository = internshipRepository;
        this.messageRepository = messageRepository;
        this.telegramBotService = telegramBotService;
        this.studentAchievementService = studentAchievementService;
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

        List<InternshipResponse> companyJobs = List.of();
        if (isVerified) {
            companyJobs = internshipRepository.findAll().stream()
                    .filter(i -> i.getCompany() != null)
                    .map(InternshipResponse::from)
                    .toList();
        }

        return Map.of(
                "user", UserResponse.from(student),
                "isVerified", isVerified,
                "applications", apps.stream().map(ApplicationResponse::from).toList(),
                "companyJobs", companyJobs
        );
    }

    @PostMapping("/apply/{id}")
    @Transactional
    public ResponseEntity<?> apply(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails principal) {
        try {
            User student = currentUser(principal);
            Internship internship = internshipRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Стажировка табылмады"));

            if (applicationRepository.existsByStudentAndInternship(student, internship)) {
                return ResponseEntity.badRequest()
                        .body(new ApiError("Сіз бұл позицияға өтінім беріп қойғансыз."));
            }

            Application application = new Application();
            application.setStudent(student);
            application.setInternship(internship);
            application.setAppliedAt(LocalDateTime.now());

            if (internship.getUniversity() != null) {
                if (internship.getJoinedCount() >= internship.getMaxPlaces()) {
                    return ResponseEntity.badRequest()
                            .body(new ApiError("Бос орындар таусылды!"));
                }
                application.setStatus(ApplicationStatus.APPROVED);
                internship.setJoinedCount(internship.getJoinedCount() + 1);
                internshipRepository.save(internship);
            } else {
                application.setStatus(ApplicationStatus.PENDING);
            }

            applicationRepository.save(application);
            return ResponseEntity.ok(ApplicationResponse.from(application));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
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
        List<InternshipResponse> jobs = internshipRepository.findAll().stream()
                .filter(i -> i.getCompany() != null)
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .map(InternshipResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("jobs", jobs));
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
        Internship internship = internshipRepository.findById(internshipId).orElseThrow();

        boolean isAccepted = applicationRepository.findByStudent(me).stream()
                .anyMatch(app -> app.getInternship().getId().equals(internshipId)
                        && (app.getStatus() == ApplicationStatus.ACCEPTED
                        || app.getStatus() == ApplicationStatus.APPROVED));

        if (!isAccepted) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("Чат недоступен"));
        }

        User companyUser = internship.getCompany().getUser();
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
        Long internshipId = Long.valueOf(body.get("internshipId").toString());
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        String content = body.get("content").toString();

        User receiver = userRepository.findById(receiverId).orElseThrow();
        Internship internship = internshipRepository.findById(internshipId).orElseThrow();

        Message msg = new Message();
        msg.setSender(me);
        msg.setReceiver(receiver);
        msg.setContent(content);
        msg.setInternship(internship);
        msg.setSentAt(LocalDateTime.now());
        messageRepository.save(msg);

        if (receiver.getTelegramChatId() != null) {
            telegramBotService.sendNotification(receiver.getTelegramChatId(),
                    "🎓 Новый вопрос от студента " + me.getUsername() +
                            " по вакансии \"" + internship.getTitle() + "\":\n\n" + content);
        }

        return MessageResponse.from(msg);
    }

}
