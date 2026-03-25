package com.example.internship.services;

import org.springframework.beans.factory.annotation.Value; // ОСЫ ИМПОРТ ДҰРЫС
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class AiService {

    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    public String generateResponse(String prompt) {
        try {
            RestTemplate rt = new RestTemplate();
            String url = "https://api.deepseek.com/chat/completions";

            Map<String, Object> requestBody = Map.of(
                    "model", "deepseek-chat",
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "stream", false
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepseekApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = rt.postForEntity(url, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                return message.get("content").toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "DeepSeek қатесі: " + e.getMessage();
        }
        return "Жауап табылмады.";
    }
}