package com.example.internship.controllers.api;

import com.example.internship.dto.*;
import com.example.internship.models.*;
import com.example.internship.repositories.*;
import com.example.internship.services.CertificateService;
import com.example.internship.services.StudentAchievementService;
import com.example.internship.services.TelegramBotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/company")
public class ApiCompanyController {

    private final InternshipRepository internshipRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final MessageRepository messageRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final TelegramBotService telegramBotService;
    private final StudentAchievementService studentAchievementService;
    private final CertificateService certificateService;

    public ApiCompanyController(InternshipRepository internshipRepository,
                                UserRepository userRepository,
                                ApplicationRepository applicationRepository,
                                CompanyRepository companyRepository,
                                MessageRepository messageRepository,
                                LessonProgressRepository lessonProgressRepository,
                                TelegramBotService telegramBotService,
                                StudentAchievementService studentAchievementService,
                                CertificateService certificateService) {
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.messageRepository = messageRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.telegramBotService = telegramBotService;
        this.studentAchievementService = studentAchievementService;
        this.certificateService = certificateService;
    }

    private User currentUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername()).orElseThrow();
    }

    private Internship requireOwnCompanyInternship(Long id, User user) {
        Company company = companyRepository.findByUserId(user.getId());
        if (company == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Сначала заполните профиль компании");
        }
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (internship.getCompany() == null || !internship.getCompany().getId().equals(company.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return internship;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        Company company = companyRepository.findByUserId(user.getId());

        if (company == null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("user", UserResponse.from(user));
            body.put("company", null);
            body.put("internships", Collections.emptyList());
            body.put("candidates", Collections.emptyList());
            return body;
        }

        List<InternshipResponse> internships = internshipRepository.findByCompanyId(company.getId())
                .stream().map(InternshipResponse::from).toList();
        List<ApplicationResponse> candidates = applicationRepository.findByInternshipCompanyId(company.getId())
                .stream().map(ApplicationResponse::from).toList();

        return Map.of(
                "user", UserResponse.from(user),
                "company", CompanyResponse.from(company),
                "internships", internships,
                "candidates", candidates
        );
    }

    @PostMapping("/internships")
    public InternshipResponse addInternship(@RequestBody Internship internship,
                                            @AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        Company company = companyRepository.findByUserId(user.getId());
        if (company == null) {
            throw new IllegalStateException("Сначала заполните профиль компании");
        }
        internship.setCompany(company);
        internship.setStatus(InternshipStatus.PENDING);
        return InternshipResponse.from(internshipRepository.save(internship));
    }

    @Transactional
    @DeleteMapping("/internships/{id}")
    public ResponseEntity<Void> deleteInternship(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails principal) {
        Internship internship = requireOwnCompanyInternship(id, currentUser(principal));
        lessonProgressRepository.deleteByApplication_Internship_Id(id);
        messageRepository.deleteByInternship(internship);
        applicationRepository.deleteByInternshipId(id);
        internshipRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    public CompanyResponse getProfile(@AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        Company company = companyRepository.findByUserId(user.getId());
        return CompanyResponse.from(company != null ? company : new Company());
    }

    @PostMapping("/profile")
    public CompanyResponse saveProfile(@RequestBody Company company,
                                       @AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        company.setUser(user);
        return CompanyResponse.from(companyRepository.save(company));
    }

    @PostMapping("/applications/{id}/accept")
    public ApplicationResponse accept(@PathVariable Long id) {
        Application app = applicationRepository.findById(id).orElseThrow();
        app.setStatus(ApplicationStatus.ACCEPTED);
        applicationRepository.save(app);
        if (app.getStudent().getTelegramChatId() != null) {
            telegramBotService.sendNotification(app.getStudent().getTelegramChatId(),
                    "✅ Компания сіздің өтініміңізді қабылдады!");
        }
        return ApplicationResponse.from(app);
    }

    @PostMapping("/applications/{id}/reject")
    public ApplicationResponse reject(@PathVariable Long id) {
        Application app = applicationRepository.findById(id).orElseThrow();
        app.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(app);
        return ApplicationResponse.from(app);
    }

    @GetMapping("/messages/{internshipId}/{studentId}")
    public Map<String, Object> chat(@PathVariable Long internshipId,
                                    @PathVariable Long studentId,
                                    @AuthenticationPrincipal UserDetails principal) {
        User me = currentUser(principal);
        User student = userRepository.findById(studentId).orElseThrow();
        List<MessageResponse> history = messageRepository
                .findByInternshipIdAndSenderIdAndReceiverIdOrInternshipIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                        internshipId, me.getId(), student.getId(),
                        internshipId, student.getId(), me.getId())
                .stream().map(MessageResponse::from).toList();
        return Map.of("history", history, "student", UserResponse.from(student), "internshipId", internshipId);
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
                    "📩 Новое сообщение от компании: " + content);
        }
        return MessageResponse.from(msg);
    }

    @GetMapping("/students/{id}")
    public StudentPublicProfileResponse studentProfile(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails principal) {
        User companyUser = currentUser(principal);
        User student = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        studentAchievementService.assertCompanyCanViewStudent(companyUser, student);
        return new StudentPublicProfileResponse(
                UserResponse.from(student),
                studentAchievementService.completedProgramsForStudent(student)
        );
    }

    @GetMapping("/students/{studentId}/certificate/{applicationId}")
    public void downloadCertificate(@PathVariable Long studentId,
                                    @PathVariable Long applicationId,
                                    @AuthenticationPrincipal UserDetails principal,
                                    HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {
        User companyUser = currentUser(principal);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        studentAchievementService.assertCompanyCanViewStudent(companyUser, student);

        Application app = studentAchievementService.requireCompletedProgram(applicationId);
        if (!app.getStudent().getId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        byte[] pdf = certificateService.generate(app, request);
        String studentPart = student.getUsername() != null
                ? student.getUsername().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "student";
        String filename = "certificate-" + studentPart + "-" + applicationId + ".pdf";
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.getOutputStream().write(pdf);
    }

    @PutMapping("/internships/{id}")
    public InternshipResponse updateInternship(@PathVariable Long id,
                                               @RequestBody Internship updated,
                                               @AuthenticationPrincipal UserDetails principal) {
        Internship existing = requireOwnCompanyInternship(id, currentUser(principal));
        existing.setTitle(updated.getTitle());
        existing.setCity(updated.getCity());
        existing.setDescription(updated.getDescription());
        existing.setStatus(InternshipStatus.PENDING);
        return InternshipResponse.from(internshipRepository.save(existing));
    }

    @PostMapping("/internships/{id}/close")
    public InternshipResponse close(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails principal) {
        Internship internship = requireOwnCompanyInternship(id, currentUser(principal));
        internship.setStatus(InternshipStatus.CLOSED);
        return InternshipResponse.from(internshipRepository.save(internship));
    }

    @PostMapping("/internships/{id}/reopen")
    public InternshipResponse reopen(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails principal) {
        Internship internship = requireOwnCompanyInternship(id, currentUser(principal));
        internship.setStatus(InternshipStatus.PENDING);
        return InternshipResponse.from(internshipRepository.save(internship));
    }

    @GetMapping("/download/resume/{studentId}")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long studentId)
            throws MalformedURLException, IOException {
        User student = userRepository.findById(studentId).orElseThrow();
        if (student.getResumePath() == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(UPLOAD_DIR).resolve(student.getResumePath()).normalize();
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String storedName = student.getResumePath();
        String displayName = storedName.contains("_")
                ? storedName.substring(storedName.indexOf('_') + 1)
                : storedName;

        Resource resource = new UrlResource(filePath.toUri());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(displayName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(resolveMediaType(displayName))
                .contentLength(Files.size(filePath))
                .body(resource);
    }

    private static final String UPLOAD_DIR = "uploads/resumes/";

    private MediaType resolveMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (lower.endsWith(".docx")) {
            return MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        if (lower.endsWith(".doc")) {
            return MediaType.parseMediaType("application/msword");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
