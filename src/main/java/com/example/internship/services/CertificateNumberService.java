package com.example.internship.services;

import com.example.internship.models.Application;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CertificateNumberService {

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("^IPRO-(\\d{4})-(\\d+)$", Pattern.CASE_INSENSITIVE);

    public String buildNumber(Application application) {
        LocalDate date = application.getCompletedAt() != null
                ? application.getCompletedAt().toLocalDate()
                : LocalDate.now();
        return format(date.getYear(), application.getId());
    }

    public String format(int year, long applicationId) {
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
        Matcher matcher = NUMBER_PATTERN.matcher(normalize(raw));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int year = Integer.parseInt(matcher.group(1));
        long applicationId = Long.parseLong(matcher.group(2));
        if (applicationId <= 0) {
            return Optional.empty();
        }
        return Optional.of(new ParsedCertificateNumber(year, applicationId, format(year, applicationId)));
    }

    public String normalize(String raw) {
        return raw.trim().toUpperCase();
    }

    public record ParsedCertificateNumber(int year, long applicationId, String formatted) {
    }
}
