package com.example.internship.controllers.api;

import com.example.internship.dto.ApplicationResponse;
import com.example.internship.dto.InternshipResponse;
import com.example.internship.dto.StudentPublicProfileResponse;
import com.example.internship.dto.UniversityResponse;
import com.example.internship.dto.UserResponse;
import com.example.internship.models.*;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.LessonProgressRepository;
import com.example.internship.repositories.MessageRepository;
import com.example.internship.repositories.ProgramLessonRepository;
import com.example.internship.repositories.ProgramMaterialRepository;
import com.example.internship.repositories.QuizQuestionRepository;
import com.example.internship.repositories.UserRepository;
import com.example.internship.services.CertificateService;
import com.example.internship.services.StudentAchievementService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/university-admin")
public class ApiUniversityAdminController {

    private final UserRepository userRepository;
    private final InternshipRepository internshipRepository;
    private final ApplicationRepository applicationRepository;
    private final ProgramLessonRepository lessonRepository;
    private final ProgramMaterialRepository materialRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final MessageRepository messageRepository;
    private final StudentAchievementService studentAchievementService;
    private final CertificateService certificateService;

    public ApiUniversityAdminController(UserRepository userRepository,
                                        InternshipRepository internshipRepository,
                                        ApplicationRepository applicationRepository,
                                        ProgramLessonRepository lessonRepository,
                                        ProgramMaterialRepository materialRepository,
                                        QuizQuestionRepository quizQuestionRepository,
                                        LessonProgressRepository lessonProgressRepository,
                                        MessageRepository messageRepository,
                                        StudentAchievementService studentAchievementService,
                                        CertificateService certificateService) {
        this.userRepository = userRepository;
        this.internshipRepository = internshipRepository;
        this.applicationRepository = applicationRepository;
        this.lessonRepository = lessonRepository;
        this.materialRepository = materialRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.messageRepository = messageRepository;
        this.studentAchievementService = studentAchievementService;
        this.certificateService = certificateService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();
        University university = user.getUniversity();
        List<Internship> myInternships = internshipRepository.findByUniversity(university);

        List<ApplicationResponse> allApplications = myInternships.stream()
                .flatMap(i -> i.getApplications().stream())
                .map(ApplicationResponse::from)
                .collect(Collectors.toList());

        return Map.of(
                "user", UserResponse.from(user),
                "university", UniversityResponse.from(university),
                "internships", myInternships.stream().map(InternshipResponse::from).toList(),
                "applications", allApplications
        );
    }

    @PostMapping("/internships")
    public InternshipResponse addInternship(@RequestBody Map<String, Object> body,
                                            @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();
        University university = user.getUniversity();
        if (university == null) {
            throw new IllegalStateException("У пользователя не привязан университет");
        }

        Internship internship = new Internship();
        internship.setTitle((String) body.get("title"));
        internship.setDescription((String) body.get("description"));
        internship.setMaxPlaces(body.get("maxPlaces") != null
                ? Integer.parseInt(body.get("maxPlaces").toString()) : 0);
        internship.setStudyMaterials((String) body.get("studyMaterials"));
        internship.setUniversity(university);
        internship.setStatus(InternshipStatus.PENDING);
        return InternshipResponse.from(internshipRepository.save(internship));
    }

    @Transactional
    @DeleteMapping("/internships/{id}")
    public void deleteInternship(@PathVariable Long id) {
        Internship internship = internshipRepository.findById(id).orElseThrow();
        lessonProgressRepository.deleteByApplication_Internship_Id(id);
        messageRepository.deleteByInternship(internship);
        applicationRepository.deleteByInternship(internship);
        lessonRepository.deleteByInternshipId(id);
        materialRepository.deleteByInternshipId(id);
        quizQuestionRepository.deleteByInternshipId(id);
        internshipRepository.deleteById(id);
    }

    @PutMapping("/internships/{id}")
    public InternshipResponse updateInternship(@PathVariable Long id,
                                               @RequestBody Internship internship,
                                               @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();
        Internship existing = requireOwnUniversityInternship(id, user);
        existing.setTitle(internship.getTitle());
        existing.setDescription(internship.getDescription());
        existing.setStudyMaterials(internship.getStudyMaterials());
        existing.setMaxPlaces(internship.getMaxPlaces());
        existing.setStatus(InternshipStatus.PENDING);
        return InternshipResponse.from(internshipRepository.save(existing));
    }

    private Internship requireOwnUniversityInternship(Long id, User user) {
        University university = user.getUniversity();
        if (university == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Internship existing = internshipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (existing.getUniversity() == null
                || !existing.getUniversity().getId().equals(university.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return existing;
    }

    @PostMapping("/applications/{id}/verify")
    public ApplicationResponse verify(@PathVariable Long id) {
        Application app = applicationRepository.findById(id).orElseThrow();
        app.setStatus(ApplicationStatus.VERIFIED);
        applicationRepository.save(app);
        return ApplicationResponse.from(app);
    }

    @GetMapping("/students/{id}")
    public StudentPublicProfileResponse studentProfile(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails principal) {
        User admin = userRepository.findByUsername(principal.getUsername()).orElseThrow();
        User student = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new StudentPublicProfileResponse(
                UserResponse.from(student),
                studentAchievementService.completedProgramsForStudentAtUniversity(student, admin.getUniversity())
        );
    }

    @GetMapping("/applications/{applicationId}/certificate")
    public void downloadCertificate(@PathVariable Long applicationId,
                                    @AuthenticationPrincipal UserDetails principal,
                                    jakarta.servlet.http.HttpServletRequest request,
                                    HttpServletResponse response) throws java.io.IOException {
        User admin = userRepository.findByUsername(principal.getUsername()).orElseThrow();
        Application app = studentAchievementService.requireCompletedProgram(applicationId);
        studentAchievementService.assertUniversityCanView(app, admin.getUniversity());

        byte[] pdf = certificateService.generate(app, request);
        User student = app.getStudent();
        String studentPart = student.getUsername() != null
                ? student.getUsername().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "student";
        String filename = "certificate-" + studentPart + "-" + applicationId + ".pdf";
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.getOutputStream().write(pdf);
    }
}
