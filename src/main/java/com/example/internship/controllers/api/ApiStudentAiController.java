package com.example.internship.controllers.api;

import com.example.internship.dto.ApiError;
import com.example.internship.dto.ai.ImproveResumeResponse;
import com.example.internship.dto.ai.JobMatchResponse;
import com.example.internship.models.User;
import com.example.internship.repositories.UserRepository;
import com.example.internship.services.StudentAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student/ai")
public class ApiStudentAiController {

    private final StudentAiService studentAiService;
    private final UserRepository userRepository;

    public ApiStudentAiController(StudentAiService studentAiService, UserRepository userRepository) {
        this.studentAiService = studentAiService;
        this.userRepository = userRepository;
    }

    private User currentUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername()).orElseThrow();
    }

    @PostMapping("/improve-resume")
    public ResponseEntity<?> improveResume(@RequestBody(required = false) Map<String, String> body,
                                           @AuthenticationPrincipal UserDetails principal) {
        try {
            User user = currentUser(principal);
            String resumeText = body != null && body.get("resume") != null && !body.get("resume").isBlank()
                    ? body.get("resume")
                    : user.getResume();
            return ResponseEntity.ok(studentAiService.improveResume(resumeText));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        } catch (com.example.internship.services.AiServiceException e) {
            return ResponseEntity.status(429).body(new ApiError(e.getMessage()));
        }
    }

    @PostMapping("/job-matches")
    public ResponseEntity<?> jobMatches(@AuthenticationPrincipal UserDetails principal) {
        try {
            User user = currentUser(principal);
            return ResponseEntity.ok(studentAiService.analyzeJobMatches(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        } catch (com.example.internship.services.AiServiceException e) {
            return ResponseEntity.status(429).body(new ApiError(e.getMessage()));
        }
    }
}
