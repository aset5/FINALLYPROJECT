package com.example.internship.services;

import com.example.internship.models.Application;
import com.example.internship.repositories.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CertificateNumberService {

    private static final int TOKEN_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Без I/O — меньше путаницы при ручном вводе номера. */
    private static final char[] TOKEN_ALPHABET = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private static final Pattern TOKEN_NUMBER_PATTERN =
            Pattern.compile("^IPRO-(\\d{4})-([A-Z0-9]{12})$", Pattern.CASE_INSENSITIVE);

    private static final Pattern LEGACY_NUMBER_PATTERN =
            Pattern.compile("^IPRO-(\\d{4})-(\\d{1,10})$", Pattern.CASE_INSENSITIVE);

    private final ApplicationRepository applicationRepository;

    public CertificateNumberService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public String buildNumber(Application application) {
        LocalDate date = application.getCompletedAt() != null
                ? application.getCompletedAt().toLocalDate()
                : LocalDate.now();
        String token = ensureCertificateToken(application);
        return format(date.getYear(), token);
    }

    @Transactional
    public String ensureCertificateToken(Application application) {
        if (application.getCertificateToken() != null && !application.getCertificateToken().isBlank()) {
            return application.getCertificateToken();
        }
        String token;
        do {
            token = generateToken();
        } while (applicationRepository.findByCertificateTokenWithDetails(token).isPresent());
        application.setCertificateToken(token);
        applicationRepository.save(application);
        return token;
    }

    @Transactional
    public void assignCertificateToken(Application application) {
        ensureCertificateToken(application);
    }

    public String format(int year, String token) {
        return String.format("IPRO-%d-%s", year, token.toUpperCase());
    }

    /** @deprecated только для обратной совместимости со старыми PDF */
    public String formatLegacy(int year, long applicationId) {
        return String.format("IPRO-%d-%06d", year, applicationId);
    }

    public String buildVerifyPath(String certNumber) {
        return "/verify/" + normalize(certNumber);
    }

    public String buildVerifyUrl(String publicBaseUrl, String certNumber) {
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return base + buildVerifyPath(certNumber);
    }

    public Optional<ParsedCertificateNumber> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(raw);

        Matcher tokenMatcher = TOKEN_NUMBER_PATTERN.matcher(normalized);
        if (tokenMatcher.matches()) {
            int year = Integer.parseInt(tokenMatcher.group(1));
            String token = tokenMatcher.group(2).toUpperCase();
            return Optional.of(ParsedCertificateNumber.byToken(year, token, format(year, token)));
        }

        Matcher legacyMatcher = LEGACY_NUMBER_PATTERN.matcher(normalized);
        if (legacyMatcher.matches()) {
            int year = Integer.parseInt(legacyMatcher.group(1));
            long applicationId = Long.parseLong(legacyMatcher.group(2));
            if (applicationId <= 0) {
                return Optional.empty();
            }
            return Optional.of(ParsedCertificateNumber.byLegacyId(
                    year, applicationId, formatLegacy(year, applicationId)));
        }

        return Optional.empty();
    }

    public String normalize(String raw) {
        return raw.trim().toUpperCase();
    }

    private String generateToken() {
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(TOKEN_ALPHABET[RANDOM.nextInt(TOKEN_ALPHABET.length)]);
        }
        String token = sb.toString();
        if (token.chars().allMatch(Character::isDigit)) {
            sb.setCharAt(0, 'A');
            token = sb.toString();
        }
        return token;
    }

    public record ParsedCertificateNumber(
            int year,
            String formatted,
            LookupType lookupType,
            String token,
            Long applicationId
    ) {
        public enum LookupType {
            TOKEN,
            LEGACY_ID
        }

        public static ParsedCertificateNumber byToken(int year, String token, String formatted) {
            return new ParsedCertificateNumber(year, formatted, LookupType.TOKEN, token, null);
        }

        public static ParsedCertificateNumber byLegacyId(int year, long applicationId, String formatted) {
            return new ParsedCertificateNumber(year, formatted, LookupType.LEGACY_ID, null, applicationId);
        }
    }
}
