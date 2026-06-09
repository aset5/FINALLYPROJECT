package com.example.internship.repositories;

import com.example.internship.models.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmailIgnoreCase(String email);
    void deleteByEmailIgnoreCase(String email);
}
