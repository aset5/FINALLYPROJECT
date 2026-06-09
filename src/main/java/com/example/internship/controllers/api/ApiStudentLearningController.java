package com.example.internship.controllers.api;

import com.example.internship.dto.*;
import com.example.internship.models.*;
import com.example.internship.repositories.MessageRepository;
import com.example.internship.repositories.UserRepository;
import com.example.internship.services.CertificateService;
import com.example.internship.services.LearningService;
import com.example.internship.services.TelegramBotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/learning")
public class ApiStudentLearningController {

    private static final String UPLOAD_DIR = "uploads/learning/";

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final LearningService learningService;
    private final CertificateService certificateService;
    private final TelegramBotService telegramBotService;

    public ApiStudentLearningController(UserRepository userRepository,
                                        MessageRepository messageRepository,
                                        LearningService learningService,
                                        CertificateService certificateService,
                                        TelegramBotService telegramBotService) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.learningService = learningService;
        this.certificateService = certificateService;
        this.telegramBotService = telegramBotService;
    }

    private User currentUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername()).orElseThrow();
    }

    @GetMapping("/{applicationId}")
    public LearningDetailResponse detail(@PathVariable Long applicationId,
                                         @AuthenticationPrincipal UserDetails principal) {
        Application app = learningService.requireStudentApplication(applicationId, principal.getUsername());
        return learningService.getLearningDetail(app);
    }

    @PostMapping("/{applicationId}/lessons/{lessonId}/complete")
    public Map<String, Object> completeLesson(@PathVariable Long applicationId,
                                              @PathVariable Long lessonId,
                                              @RequestBody(required = false) Map<String, Integer> body,
                                              @AuthenticationPrincipal UserDetails principal) {
        Application app = learningService.requireStudentApplication(applicationId, principal.getUsername());
        Integer answerIndex = body != null ? body.get("answerIndex") : null;
        return learningService.completeLesson(app, lessonId, answerIndex);
    }

    @PostMapping("/{applicationId}/quiz/submit")
    public Map<String, Object> submitQuiz(@PathVariable Long applicationId,
                                          @RequestBody Map<String, Integer> answers,
                                          @AuthenticationPrincipal UserDetails principal) {
        Application app = learningService.requireStudentApplication(applicationId, principal.getUsername());
        return learningService.submitQuiz(app, answers);
    }

    @PostMapping("/{applicationId}/complete")
    public ApplicationResponse completeProgram(@PathVariable Long applicationId,
                                               @AuthenticationPrincipal UserDetails principal) {
        Application app = learningService.requireStudentApplication(applicationId, principal.getUsername());
        return learningService.completeProgram(app);
    }

    @GetMapping("/{applicationId}/certificate")
    public void certificate(@PathVariable Long applicationId,
                            @AuthenticationPrincipal UserDetails principal,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        Application app = learningService.requireStudentApplication(applicationId, principal.getUsername());
        if (app.getStatus() != ApplicationStatus.COMPLETED && app.getStatus() != ApplicationStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Сертификат доступен после завершения");
        }
        byte[] pdf = certificateService.generate(app, request);
        String studentPart = app.getStudent().getUsername() != null
                ? app.getStudent().getUsername().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "student";
        String filename = "certificate-" + studentPart + "-" + applicationId + ".pdf";
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/files/{storedName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String storedName,
                                                   @AuthenticationPrincipal UserDetails principal) throws IOException {
        currentUser(principal);
        if (storedName.contains("..") || storedName.contains("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Path path = Paths.get(UPLOAD_DIR).resolve(storedName);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Resource resource = new UrlResource(path.toUri());
        String displayName = storedName.contains("_")
                ? storedName.substring(storedName.indexOf('_') + 1)
                : storedName;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + displayName + "\"")
                .body(resource);
    }

    @GetMapping("/{applicationId}/messages")
    public ResponseEntity<?> universityChat(@PathVariable Long applicationId,
                                            @AuthenticationPrincipal UserDetails principal) {
        Application app = learningService.requireStudentApplication(applicationId, principal.getUsername());
        User me = app.getStudent();
        Internship internship = app.getInternship();
        User universityUser = learningService.universityContact(internship);

        List<MessageResponse> history = messageRepository
                .findByInternshipIdAndSenderIdAndReceiverIdOrInternshipIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                        internship.getId(), me.getId(), universityUser.getId(),
                        internship.getId(), universityUser.getId(), me.getId())
                .stream()
                .map(MessageResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "history", history,
                "universityUser", UserResponse.from(universityUser),
                "internshipId", internship.getId(),
                "internshipTitle", internship.getTitle(),
                "currentUsername", me.getUsername()
        ));
    }

    @PostMapping("/{applicationId}/messages")
    public MessageResponse sendUniversityMessage(@PathVariable Long applicationId,
                                                 @RequestBody Map<String, String> body,
                                                 @AuthenticationPrincipal UserDetails principal) {
        Application app = learningService.requireStudentApplication(applicationId, principal.getUsername());
        User me = app.getStudent();
        Internship internship = app.getInternship();
        User receiver = learningService.universityContact(internship);
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пустое сообщение");
        }

        Message msg = new Message();
        msg.setSender(me);
        msg.setReceiver(receiver);
        msg.setContent(content.trim());
        msg.setInternship(internship);
        msg.setSentAt(LocalDateTime.now());
        messageRepository.save(msg);

        if (receiver.getTelegramChatId() != null) {
            telegramBotService.sendNotification(receiver.getTelegramChatId(),
                    "📚 Вопрос от студента " + me.getUsername() +
                            " по программе \"" + internship.getTitle() + "\":\n\n" + content);
        }
        return MessageResponse.from(msg);
    }
}
