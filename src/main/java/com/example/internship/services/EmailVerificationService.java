package com.example.internship.services;

import com.example.internship.models.EmailVerification;
import com.example.internship.repositories.EmailVerificationRepository;
import com.example.internship.repositories.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private static final int CODE_TTL_MINUTES = 15;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(EmailVerificationRepository verificationRepository,
                                  UserRepository userRepository,
                                  EmailService emailService) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendCode(String rawEmail) throws MessagingException {
        String email = normalizeEmail(rawEmail);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Этот email уже зарегистрирован");
        }

        var existing = verificationRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null && existing.getCreatedAt() != null) {
            LocalDateTime nextAllowed = existing.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
            if (LocalDateTime.now().isBefore(nextAllowed)) {
                long wait = java.time.Duration.between(LocalDateTime.now(), nextAllowed).getSeconds();
                throw new IllegalArgumentException("Повторная отправка через " + wait + " сек.");
            }
        }

        String code = generateCode();
        EmailVerification verification = existing != null ? existing : new EmailVerification();
        verification.setEmail(email);
        verification.setCode(code);
        verification.setVerified(false);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
        verification.setCreatedAt(LocalDateTime.now());
        verificationRepository.save(verification);

        emailService.sendVerificationCode(email, code);
    }

    @Transactional
    public boolean verifyCode(String rawEmail, String code) {
        String email = normalizeEmail(rawEmail);
        EmailVerification verification = verificationRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Сначала запросите код на email"));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Код истёк. Запросите новый.");
        }

        if (!verification.getCode().equals(code.trim())) {
            throw new IllegalArgumentException("Неверный код подтверждения");
        }

        verification.setVerified(true);
        verificationRepository.save(verification);
        return true;
    }

    @Transactional
    public void requireVerifiedEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        EmailVerification verification = verificationRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Подтвердите email перед регистрацией"));

        if (!verification.isVerified()) {
            throw new IllegalArgumentException("Email не подтверждён. Введите код из письма.");
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Сессия подтверждения истекла. Запросите код снова.");
        }
    }

    @Transactional
    public void consumeVerification(String rawEmail) {
        verificationRepository.deleteByEmailIgnoreCase(normalizeEmail(rawEmail));
    }

    private String generateCode() {
        int num = 100000 + random.nextInt(900000);
        return String.valueOf(num);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
