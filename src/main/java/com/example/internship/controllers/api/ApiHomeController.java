package com.example.internship.controllers.api;

import com.example.internship.dto.InternshipResponse;
import com.example.internship.models.ApplicationStatus;
import com.example.internship.models.Internship;
import com.example.internship.models.InternshipStatus;
import com.example.internship.models.User;
import com.example.internship.repositories.ApplicationRepository;
import com.example.internship.repositories.InternshipRepository;
import com.example.internship.repositories.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiHomeController {

    private final InternshipRepository internshipRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public ApiHomeController(InternshipRepository internshipRepository,
                             UserRepository userRepository,
                             ApplicationRepository applicationRepository) {
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/home")
    public Map<String, Object> home(@AuthenticationPrincipal UserDetails principal) {
        Long studentUniId = null;
        boolean isVerified = false;

        if (principal != null) {
            User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
            if (user != null) {
                isVerified = applicationRepository.existsByStudentIdAndStatus(
                        user.getId(), ApplicationStatus.VERIFIED);
                if (user.getUniversity() != null) {
                    studentUniId = user.getUniversity().getId();
                }
            }
        }

        final Long finalStudentUniId = studentUniId;
        List<InternshipResponse> universityPrograms = internshipRepository.findAll().stream()
                .filter(i -> i.getUniversity() != null)
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .filter(i -> finalStudentUniId != null
                        && i.getUniversity().getId().equals(finalStudentUniId))
                .map(InternshipResponse::from)
                .toList();

        List<InternshipResponse> companyJobs = internshipRepository.findAll().stream()
                .filter(i -> i.getCompany() != null)
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .map(InternshipResponse::from)
                .toList();

        return Map.of(
                "uniPrograms", universityPrograms,
                "companyJobs", companyJobs,
                "isVerified", isVerified
        );
    }
}
