package com.example.internship.services;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Базовый URL для QR на сертификате.
 * <ul>
 *   <li>{@code auto} — из текущего HTTP-запроса (откройте сайт как http://192.168.x.x:8080)</li>
 *   <li>явный URL — для продакшена или фиксированного IP в Wi‑Fi</li>
 * </ul>
 */
@Component
public class PublicBaseUrlResolver {

    private final String configured;

    public PublicBaseUrlResolver(
            @Value("${app.public-base-url:auto}") String configured) {
        this.configured = configured != null ? configured.trim() : "auto";
    }

    public String resolve(HttpServletRequest request) {
        if (!isAutoMode() && !configured.isEmpty()) {
            return stripTrailingSlash(configured);
        }
        if (request != null) {
            return stripTrailingSlash(buildFromRequest(request));
        }
        return "http://localhost:8080";
    }

    public boolean isAutoMode() {
        return configured.isEmpty() || "auto".equalsIgnoreCase(configured);
    }

    private String buildFromRequest(HttpServletRequest req) {
        String forwardedHost = firstHeaderValue(req.getHeader("X-Forwarded-Host"));
        if (forwardedHost != null) {
            String scheme = firstHeaderValue(req.getHeader("X-Forwarded-Proto"));
            if (scheme == null || scheme.isBlank()) {
                scheme = "https";
            }
            return scheme + "://" + forwardedHost;
        }

        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        if (isDefaultPort(scheme, port)) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
    }

    private static String firstHeaderValue(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        return header.split(",")[0].trim();
    }

    private static String stripTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
