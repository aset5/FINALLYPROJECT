package com.example.internship.controllers.api;

import com.example.internship.dto.*;
import com.example.internship.models.Role;
import com.example.internship.models.University;
import com.example.internship.models.User;
import com.example.internship.repositories.UniversityRepository;
import com.example.internship.repositories.UserRepository;
import com.example.internship.services.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public ApiAuthController(UserRepository userRepository,
                             UniversityRepository universityRepository,
                             PasswordEncoder passwordEncoder,
                             EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.universityRepository = universityRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return userRepository.findByUsername(principal.getUsername())
                .map(user -> {
                    if (!user.isEnabled()) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<UserResponse>build();
                    }
                    return ResponseEntity.ok(UserResponse.from(user));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/universities")
    public List<UniversityResponse> universities() {
        return universityRepository.findAll().stream()
                .map(UniversityResponse::from)
                .toList();
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody SendEmailCodeRequest request) {
        try {
            emailVerificationService.sendCode(request.email());
            return ResponseEntity.ok(Map.of(
                    "message", "Код отправлен на email. Проверьте почту (и папку «Спам»)."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Не удалось отправить письмо: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-email-code")
    public ResponseEntity<?> verifyEmailCode(@Valid @RequestBody VerifyEmailCodeRequest request) {
        try {
            emailVerificationService.verifyCode(request.email(), request.code());
            return ResponseEntity.ok(Map.of("message", "Email подтверждён", "verified", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().body(new ApiError("Бұл логин бос емес!"));
        }

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            return ResponseEntity.badRequest().body(new ApiError("Этот email уже зарегистрирован"));
        }

        try {
            emailVerificationService.requireVerifiedEmail(request.email());
            emailVerificationService.verifyCode(request.email(), request.verificationCode());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }

        String passwordPattern = "^(?=.*[A-Z]).{8,}$";
        if (!request.password().matches(passwordPattern)) {
            return ResponseEntity.badRequest().body(new ApiError(
                    "Пароль кемінде 8 символ және бір үлкен әріптен тұруы керек!"));
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email().trim().toLowerCase());

        String roleType = request.roleType();
        boolean requiresAdminApproval;
        if ("UNIVERSITY".equalsIgnoreCase(roleType)) {
            user.setRole(Role.UNIVERSITY_ADMIN);
            requiresAdminApproval = true;
            if (request.uniName() != null && !request.uniName().trim().isEmpty()) {
                University newUni = new University();
                newUni.setName(request.uniName().trim());
                universityRepository.save(newUni);
                user.setUniversity(newUni);
            }
        } else if ("STUDENT".equalsIgnoreCase(roleType)) {
            user.setRole(Role.STUDENT);
            requiresAdminApproval = false;
            if (request.universityId() != null) {
                universityRepository.findById(request.universityId()).ifPresent(user::setUniversity);
            }
        } else if ("COMPANY".equalsIgnoreCase(roleType)) {
            user.setRole(Role.COMPANY);
            requiresAdminApproval = true;
        } else {
            return ResponseEntity.badRequest().body(new ApiError("Жарамсыз рөл түрі"));
        }

        user.setEnabled(!requiresAdminApproval);

        try {
            userRepository.save(user);
            emailVerificationService.consumeVerification(request.email());
            String message = requiresAdminApproval
                    ? "Заявка принята. После проверки администратором вы сможете войти в систему."
                    : "Регистрация успешна";
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", message,
                    "pendingApproval", requiresAdminApproval
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiError("Қате: " + e.getMessage()));
        }
    }
}
