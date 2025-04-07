package com.example.backend.dto;

import java.util.List;
import java.util.Map;

public class OpenAiRequest {
    private String model = "gpt-4o-mini";
    private List<Map<String, String>> messages;

    public OpenAiRequest(String prompt) {
        this.messages = List.of(
            Map.of("role", "system", "content", "Odpowiadaj zawsze prostym językiem dla licealisty."),
            Map.of("role", "user", "content", prompt)
        );
    }

    public String getModel() {
        return model;
    }

    public List<Map<String, String>> getMessages() {
        return messages;
    }
}
