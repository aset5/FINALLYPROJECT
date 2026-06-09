package com.example.internship.controllers.api;

import com.example.internship.dto.CertificateVerificationResponse;
import com.example.internship.services.CertificateVerificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
public class ApiCertificateController {

    private final CertificateVerificationService verificationService;

    public ApiCertificateController(CertificateVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping("/verify/{certNumber}")
    public CertificateVerificationResponse verify(@PathVariable String certNumber) {
        return verificationService.verify(certNumber);
    }
}
