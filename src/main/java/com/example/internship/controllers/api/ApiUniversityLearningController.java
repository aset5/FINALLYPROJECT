package com.example.internship.controllers.api;

import com.example.internship.dto.*;
import com.example.internship.models.*;
import com.example.internship.repositories.*;
import com.example.internship.services.LearningService;
import com.example.internship.services.TelegramBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/university-admin/learning")
public class ApiUniversityLearningController {

    private static final String UPLOAD_DIR = "uploads/learning/";

    private final UserRepository userRepository;
    private final ProgramLessonRepository lessonRepository;
    private final ProgramMaterialRepository materialRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final MessageRepository messageRepository;
    private final LearningService learningService;
    private final TelegramBotService telegramBotService;

    public ApiUniversityLearningController(UserRepository userRepository,
                                           ProgramLessonRepository lessonRepository,
                                           ProgramMaterialRepository materialRepository,
                                           QuizQuestionRepository quizQuestionRepository,
                                           MessageRepository messageRepository,
                                           LearningService learningService,
                                           TelegramBotService telegramBotService) {
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.materialRepository = materialRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.messageRepository = messageRepository;
        this.learningService = learningService;
        this.telegramBotService = telegramBotService;
    }

    private User admin(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername()).orElseThrow();
    }

    @GetMapping("/internships/{internshipId}")
    public Map<String, Object> programContent(@PathVariable Long internshipId,
                                              @AuthenticationPrincipal UserDetails principal) {
        Internship internship = learningService.loadInternshipForAdmin(internshipId, admin(principal));
        List<ProgramLessonResponse> lessons = lessonRepository
                .findByInternshipIdOrderBySortOrderAscIdAsc(internshipId)
                .stream()
                .map(l -> ProgramLessonResponse.from(l, null, true))
                .toList();
        List<ProgramMaterialResponse> materials = materialRepository
                .findByInternshipIdOrderBySortOrderAscIdAsc(internshipId)
                .stream()
                .map(ProgramMaterialResponse::from)
                .toList();
        List<QuizQuestionResponse> quiz = quizQuestionRepository
                .findByInternshipIdOrderBySortOrderAscIdAsc(internshipId)
                .stream()
                .map(QuizQuestionResponse::forAdmin)
                .toList();
        return Map.of(
                "internship", InternshipResponse.from(internship),
                "lessons", lessons,
                "materials", materials,
                "quizQuestions", quiz
        );
    }

    @PostMapping("/internships/{internshipId}/lessons")
    public ProgramLessonResponse addLesson(@PathVariable Long internshipId,
                                           @RequestBody Map<String, Object> body,
                                           @AuthenticationPrincipal UserDetails principal) {
        Internship internship = learningService.loadInternshipForAdmin(internshipId, admin(principal));
        ProgramLesson lesson = new ProgramLesson();
        lesson.setInternship(internship);
        lesson.setTitle((String) body.get("title"));
        lesson.setContent((String) body.getOrDefault("content", ""));
        lesson.setExternalUrl((String) body.get("externalUrl"));
        lesson.setSortOrder(body.get("sortOrder") != null
                ? Integer.parseInt(body.get("sortOrder").toString())
                : nextLessonOrder(internshipId));
        applyCheckQuestion(lesson, body);
        ProgramLesson saved = lessonRepository.save(lesson);
        learningService.requireReModeration(internship);
        return ProgramLessonResponse.from(saved, null, true);
    }

    @PutMapping("/lessons/{lessonId}")
    public ProgramLessonResponse updateLesson(@PathVariable Long lessonId,
                                              @RequestBody Map<String, Object> body,
                                              @AuthenticationPrincipal UserDetails principal) {
        ProgramLesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        learningService.assertAdminOwnsInternship(lesson.getInternship(), admin(principal));
        if (body.containsKey("title")) lesson.setTitle((String) body.get("title"));
        if (body.containsKey("content")) lesson.setContent((String) body.get("content"));
        if (body.containsKey("externalUrl")) lesson.setExternalUrl((String) body.get("externalUrl"));
        if (body.containsKey("sortOrder")) {
            lesson.setSortOrder(Integer.parseInt(body.get("sortOrder").toString()));
        }
        applyCheckQuestion(lesson, body);
        ProgramLesson saved = lessonRepository.save(lesson);
        learningService.requireReModeration(lesson.getInternship());
        return ProgramLessonResponse.from(saved, null, true);
    }

    @PostMapping("/lessons/{lessonId}/file")
    public ProgramLessonResponse uploadLessonFile(@PathVariable Long lessonId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @AuthenticationPrincipal UserDetails principal) throws IOException {
        ProgramLesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        learningService.assertAdminOwnsInternship(lesson.getInternship(), admin(principal));
        lesson.setFilePath(storeFile(file));
        ProgramLesson saved = lessonRepository.save(lesson);
        learningService.requireReModeration(lesson.getInternship());
        return ProgramLessonResponse.from(saved, null, true);
    }

    @DeleteMapping("/lessons/{lessonId}")
    public void deleteLesson(@PathVariable Long lessonId,
                             @AuthenticationPrincipal UserDetails principal) {
        ProgramLesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        Internship internship = lesson.getInternship();
        learningService.assertAdminOwnsInternship(internship, admin(principal));
        lessonRepository.delete(lesson);
        learningService.requireReModeration(internship);
    }

    @PostMapping("/internships/{internshipId}/materials")
    public ProgramMaterialResponse addMaterial(@PathVariable Long internshipId,
                                               @RequestBody Map<String, Object> body,
                                               @AuthenticationPrincipal UserDetails principal) {
        Internship internship = learningService.loadInternshipForAdmin(internshipId, admin(principal));
        ProgramMaterial material = new ProgramMaterial();
        material.setInternship(internship);
        material.setTitle((String) body.get("title"));
        String typeStr = (String) body.getOrDefault("type", "LINK");
        material.setType(MaterialType.valueOf(typeStr));
        material.setUrl((String) body.get("url"));
        material.setSortOrder(body.get("sortOrder") != null
                ? Integer.parseInt(body.get("sortOrder").toString())
                : nextMaterialOrder(internshipId));
        ProgramMaterial saved = materialRepository.save(material);
        learningService.requireReModeration(internship);
        return ProgramMaterialResponse.from(saved);
    }

    @PutMapping("/materials/{materialId}")
    public ProgramMaterialResponse updateMaterial(@PathVariable Long materialId,
                                                  @RequestBody Map<String, Object> body,
                                                  @AuthenticationPrincipal UserDetails principal) {
        ProgramMaterial material = materialRepository.findById(materialId).orElseThrow();
        learningService.assertAdminOwnsInternship(material.getInternship(), admin(principal));
        if (body.containsKey("title")) {
            material.setTitle((String) body.get("title"));
        }
        if (body.containsKey("url")) {
            material.setUrl((String) body.get("url"));
        }
        if (body.containsKey("sortOrder")) {
            material.setSortOrder(Integer.parseInt(body.get("sortOrder").toString()));
        }
        ProgramMaterial saved = materialRepository.save(material);
        learningService.requireReModeration(material.getInternship());
        return ProgramMaterialResponse.from(saved);
    }

    @PostMapping("/materials/{materialId}/file")
    public ProgramMaterialResponse uploadMaterialFile(@PathVariable Long materialId,
                                                      @RequestParam("file") MultipartFile file,
                                                      @AuthenticationPrincipal UserDetails principal) throws IOException {
        ProgramMaterial material = materialRepository.findById(materialId).orElseThrow();
        learningService.assertAdminOwnsInternship(material.getInternship(), admin(principal));
        material.setType(MaterialType.FILE);
        material.setFilePath(storeFile(file));
        ProgramMaterial saved = materialRepository.save(material);
        learningService.requireReModeration(material.getInternship());
        return ProgramMaterialResponse.from(saved);
    }

    @DeleteMapping("/materials/{materialId}")
    public void deleteMaterial(@PathVariable Long materialId,
                               @AuthenticationPrincipal UserDetails principal) {
        ProgramMaterial material = materialRepository.findById(materialId).orElseThrow();
        Internship internship = material.getInternship();
        learningService.assertAdminOwnsInternship(internship, admin(principal));
        materialRepository.delete(material);
        learningService.requireReModeration(internship);
    }

    @PostMapping("/internships/{internshipId}/quiz")
    public QuizQuestionResponse addQuizQuestion(@PathVariable Long internshipId,
                                                @RequestBody Map<String, Object> body,
                                                @AuthenticationPrincipal UserDetails principal) {
        Internship internship = learningService.loadInternshipForAdmin(internshipId, admin(principal));
        QuizQuestion q = new QuizQuestion();
        q.setInternship(internship);
        q.setQuestionText((String) body.get("questionText"));
        List<?> options = (List<?>) body.get("options");
        if (options == null || options.size() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно 4 варианта ответа");
        }
        q.setOptionA(options.get(0).toString());
        q.setOptionB(options.get(1).toString());
        q.setOptionC(options.get(2).toString());
        q.setOptionD(options.get(3).toString());
        q.setCorrectIndex(Integer.parseInt(body.get("correctIndex").toString()));
        q.setSortOrder(body.get("sortOrder") != null
                ? Integer.parseInt(body.get("sortOrder").toString())
                : nextQuizOrder(internshipId));
        QuizQuestion saved = quizQuestionRepository.save(q);
        learningService.requireReModeration(internship);
        return QuizQuestionResponse.forAdmin(saved);
    }

    @PutMapping("/quiz/{questionId}")
    public QuizQuestionResponse updateQuizQuestion(@PathVariable Long questionId,
                                                 @RequestBody Map<String, Object> body,
                                                 @AuthenticationPrincipal UserDetails principal) {
        QuizQuestion q = quizQuestionRepository.findById(questionId).orElseThrow();
        learningService.assertAdminOwnsInternship(q.getInternship(), admin(principal));
        if (body.containsKey("questionText")) {
            q.setQuestionText((String) body.get("questionText"));
        }
        if (body.containsKey("options")) {
            List<?> options = (List<?>) body.get("options");
            if (options == null || options.size() < 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно 4 варианта ответа");
            }
            q.setOptionA(options.get(0).toString());
            q.setOptionB(options.get(1).toString());
            q.setOptionC(options.get(2).toString());
            q.setOptionD(options.get(3).toString());
        }
        if (body.containsKey("correctIndex")) {
            q.setCorrectIndex(Integer.parseInt(body.get("correctIndex").toString()));
        }
        if (body.containsKey("sortOrder")) {
            q.setSortOrder(Integer.parseInt(body.get("sortOrder").toString()));
        }
        QuizQuestion saved = quizQuestionRepository.save(q);
        learningService.requireReModeration(q.getInternship());
        return QuizQuestionResponse.forAdmin(saved);
    }

    @DeleteMapping("/quiz/{questionId}")
    public void deleteQuizQuestion(@PathVariable Long questionId,
                                   @AuthenticationPrincipal UserDetails principal) {
        QuizQuestion q = quizQuestionRepository.findById(questionId).orElseThrow();
        Internship internship = q.getInternship();
        learningService.assertAdminOwnsInternship(internship, admin(principal));
        quizQuestionRepository.delete(q);
        learningService.requireReModeration(internship);
    }

    @GetMapping("/messages/{internshipId}/{studentId}")
    public ResponseEntity<?> chat(@PathVariable Long internshipId,
                                  @PathVariable Long studentId,
                                  @AuthenticationPrincipal UserDetails principal) {
        User me = admin(principal);
        learningService.loadInternshipForAdmin(internshipId, me);
        User student = userRepository.findById(studentId).orElseThrow();

        List<MessageResponse> history = messageRepository
                .findByInternshipIdAndSenderIdAndReceiverIdOrInternshipIdAndSenderIdAndReceiverIdOrderBySentAtAsc(
                        internshipId, me.getId(), student.getId(),
                        internshipId, student.getId(), me.getId())
                .stream()
                .map(MessageResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "history", history,
                "student", UserResponse.from(student),
                "internshipId", internshipId
        ));
    }

    @PostMapping("/messages")
    public MessageResponse sendMessage(@RequestBody Map<String, Object> body,
                                       @AuthenticationPrincipal UserDetails principal) {
        User me = admin(principal);
        Long internshipId = Long.valueOf(body.get("internshipId").toString());
        Long studentId = Long.valueOf(body.get("studentId").toString());
        String content = body.get("content").toString();
        Internship internship = learningService.loadInternshipForAdmin(internshipId, me);
        User student = userRepository.findById(studentId).orElseThrow();

        Message msg = new Message();
        msg.setSender(me);
        msg.setReceiver(student);
        msg.setContent(content);
        msg.setInternship(internship);
        msg.setSentAt(LocalDateTime.now());
        messageRepository.save(msg);

        if (student.getTelegramChatId() != null) {
            telegramBotService.sendNotification(student.getTelegramChatId(),
                    "📚 Ответ куратора по программе \"" + internship.getTitle() + "\":\n\n" + content);
        }
        return MessageResponse.from(msg);
    }

    @SuppressWarnings("unchecked")
    private void applyCheckQuestion(ProgramLesson lesson, Map<String, Object> body) {
        if (!body.containsKey("checkQuestion")) {
            return;
        }
        String question = (String) body.get("checkQuestion");
        if (question == null || question.isBlank()) {
            lesson.setCheckQuestion(null);
            lesson.setCheckOptionA(null);
            lesson.setCheckOptionB(null);
            lesson.setCheckOptionC(null);
            lesson.setCheckOptionD(null);
            lesson.setCheckCorrectIndex(null);
            return;
        }
        lesson.setCheckQuestion(question.trim());
        List<?> options = (List<?>) body.get("checkOptions");
        if (options == null || options.size() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Для контрольного вопроса нужно 4 варианта ответа");
        }
        lesson.setCheckOptionA(options.get(0).toString());
        lesson.setCheckOptionB(options.get(1).toString());
        lesson.setCheckOptionC(options.get(2).toString());
        lesson.setCheckOptionD(options.get(3).toString());
        lesson.setCheckCorrectIndex(Integer.parseInt(body.get("checkCorrectIndex").toString()));
    }

    private int nextLessonOrder(Long internshipId) {
        return lessonRepository.findByInternshipIdOrderBySortOrderAscIdAsc(internshipId).size();
    }

    private int nextMaterialOrder(Long internshipId) {
        return materialRepository.findByInternshipIdOrderBySortOrderAscIdAsc(internshipId).size();
    }

    private int nextQuizOrder(Long internshipId) {
        return quizQuestionRepository.findByInternshipIdOrderBySortOrderAscIdAsc(internshipId).size();
    }

    private String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл пустой");
        }
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }
}
