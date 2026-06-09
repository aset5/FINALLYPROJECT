package com.example.internship.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {

    private static final Pattern RETRY_AFTER_JSON = Pattern.compile("\"retry_after_seconds\"\\s*:\\s*(\\d+)");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.api.url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${ai.api.key:${deepseek.api.key:}}")
    private String apiKey;

    @Value("${ai.model:openrouter/free}")
    private String model;

    @Value("${ai.fallback-models:openrouter/free,google/gemma-2-9b-it:free,qwen/qwen-2.5-7b-instruct:free}")
    private String fallbackModels;

    @Value("${ai.retry.max-attempts:2}")
    private int maxAttemptsPerModel;

    @Value("${ai.http-referer:http://localhost:8080}")
    private String httpReferer;

    @Value("${ai.app-title:INTERN.PRO}")
    private String appTitle;

    public String generateResponse(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException("AI не настроен: укажите ai.api.key в application-local.properties");
        }

        List<String> models = buildModelQueue();
        String lastError = null;

        for (String currentModel : models) {
            for (int attempt = 1; attempt <= maxAttemptsPerModel; attempt++) {
                try {
                    return callModel(currentModel, prompt);
                } catch (HttpClientErrorException.TooManyRequests e) {
                    lastError = extractErrorMessage(e);
                    long waitSec = parseRetryAfterSeconds(e);
                    System.out.println("AI 429 model=" + currentModel + " attempt=" + attempt + " wait=" + waitSec + "s");
                    if (attempt < maxAttemptsPerModel && waitSec > 0 && waitSec <= 60) {
                        sleepSeconds(waitSec);
                    } else {
                        break;
                    }
                } catch (HttpClientErrorException e) {
                    lastError = extractErrorMessage(e);
                    System.out.println("AI HTTP " + e.getStatusCode() + " model=" + currentModel);
                    break;
                } catch (Exception e) {
                    lastError = e.getMessage();
                    e.printStackTrace();
                    break;
                }
            }
        }

        throw new AiServiceException(
                "Сервис AI временно перегружен (лимит бесплатных моделей). "
                        + "Подождите 30–60 секунд и попробуйте снова."
                        + (lastError != null ? " Детали: " + shorten(lastError, 120) : "")
        );
    }

    private List<String> buildModelQueue() {
        LinkedHashSet<String> queue = new LinkedHashSet<>();
        if (model != null && !model.isBlank()) {
            queue.add(model.trim());
        }
        if (fallbackModels != null) {
            for (String m : fallbackModels.split(",")) {
                String trimmed = m.trim();
                if (!trimmed.isEmpty()) {
                    queue.add(trimmed);
                }
            }
        }
        if (queue.isEmpty()) {
            queue.add("openrouter/free");
        }
        return new ArrayList<>(queue);
    }

    private String callModel(String currentModel, String prompt) {
        RestTemplate rt = new RestTemplate();

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", currentModel);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        if (apiUrl.contains("openrouter.ai")) {
            headers.set("HTTP-Referer", httpReferer);
            headers.set("X-Title", appTitle);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = rt.postForEntity(apiUrl, entity, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            return message.get("content").toString();
        }
        throw new AiServiceException("Пустой ответ от модели " + currentModel);
    }

    private long parseRetryAfterSeconds(HttpClientErrorException e) {
        HttpHeaders headers = e.getResponseHeaders();
        if (headers != null && headers.getFirst("Retry-After") != null) {
            try {
                return Long.parseLong(Objects.requireNonNull(headers.getFirst("Retry-After")));
            } catch (NumberFormatException ignored) {
            }
        }
        String body = e.getResponseBodyAsString();
        if (body != null) {
            Matcher m = RETRY_AFTER_JSON.matcher(body);
            if (m.find()) {
                return Long.parseLong(m.group(1));
            }
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode sec = root.path("error").path("metadata").path("retry_after_seconds");
                if (sec.isNumber()) {
                    return sec.asLong();
                }
            } catch (Exception ignored) {
            }
        }
        return 30;
    }

    private String extractErrorMessage(HttpClientErrorException e) {
        String body = e.getResponseBodyAsString();
        if (body != null && body.contains("message")) {
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode msgNode = root.path("error").path("message");
                if (!msgNode.isMissingNode() && !msgNode.asText().isBlank()) {
                    return msgNode.asText();
                }
            } catch (Exception ignored) {
            }
        }
        return e.getMessage();
    }

    private void sleepSeconds(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String shorten(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    public String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.contains("```")) {
            int start = trimmed.indexOf("```");
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                String block = trimmed.substring(start + 3, end).trim();
                if (block.toLowerCase().startsWith("json")) {
                    block = block.substring(4).trim();
                }
                trimmed = block;
            }
        }
        int first = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return trimmed.substring(first, last + 1);
        }
        return trimmed;
    }
}
