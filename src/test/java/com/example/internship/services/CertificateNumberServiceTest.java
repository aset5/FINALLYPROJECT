package com.example.internship.services;

import com.example.internship.models.Application;
import com.example.internship.repositories.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateNumberServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    private CertificateNumberService service;

    @BeforeEach
    void setUp() {
        service = new CertificateNumberService(applicationRepository);
    }

    @Test
    void parse_acceptsTokenFormat() {
        var parsed = service.parse("ipro-2026-k7m9p2xq4r1n");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().formatted()).isEqualTo("IPRO-2026-K7M9P2XQ4R1N");
        assertThat(parsed.get().lookupType())
                .isEqualTo(CertificateNumberService.ParsedCertificateNumber.LookupType.TOKEN);
        assertThat(parsed.get().token()).isEqualTo("K7M9P2XQ4R1N");
    }

    @Test
    void parse_acceptsLegacyFormat() {
        var parsed = service.parse("IPRO-2026-42");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().applicationId()).isEqualTo(42L);
        assertThat(parsed.get().lookupType())
                .isEqualTo(CertificateNumberService.ParsedCertificateNumber.LookupType.LEGACY_ID);
    }

    @Test
    void parse_rejectsInvalidFormat() {
        assertThat(service.parse("FAKE-123")).isEmpty();
        assertThat(service.parse("")).isEmpty();
        assertThat(service.parse(null)).isEmpty();
    }

    @Test
    void buildNumber_usesExistingToken() {
        Application application = new Application();
        application.setId(7L);
        application.setCompletedAt(LocalDateTime.of(2026, 6, 1, 12, 0));
        application.setCertificateToken("ABCD1234EFGH");

        assertThat(service.buildNumber(application)).isEqualTo("IPRO-2026-ABCD1234EFGH");
    }

    @Test
    void ensureCertificateToken_generatesUniqueToken() {
        Application application = new Application();
        application.setId(1L);
        application.setCompletedAt(LocalDateTime.now());

        when(applicationRepository.findByCertificateTokenWithDetails(anyString())).thenReturn(Optional.empty());
        when(applicationRepository.save(application)).thenAnswer(invocation -> invocation.getArgument(0));

        String token = service.ensureCertificateToken(application);

        assertThat(token).hasSize(12);
        assertThat(application.getCertificateToken()).isEqualTo(token);
    }
}
