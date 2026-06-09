package com.example.internship.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class JsonResponses {

    public static final String JSON_UTF8 = "application/json;charset=UTF-8";

    private JsonResponses() {
    }

    public static void write(ObjectMapper mapper, HttpServletResponse response, int status, Map<String, ?> body)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(JSON_UTF8);
        mapper.writeValue(response.getWriter(), body);
    }
}
