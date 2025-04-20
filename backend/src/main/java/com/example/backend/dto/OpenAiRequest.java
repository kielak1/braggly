// OpenAiRequest.java
package com.example.backend.dto;

import java.util.List;
import java.util.Map;

public class OpenAiRequest {
    private String model;
    private List<Map<String, String>> messages;
    private Double temperature;
    private Integer max_tokens;

    public OpenAiRequest(String prompt) {
        // Pobieramy wartości z zmiennych środowiskowych
        this.model = System.getenv("OPENAI_MODEL");
        this.temperature = Double.parseDouble(System.getenv("OPENAI_TEMPERATURE"));
        this.max_tokens = Integer.parseInt(System.getenv("OPENAI_MAX_TOKENS"));
        // Tworzymy listę wiadomości zgodnie z API Chat Completions
        this.messages = List.of(
                Map.of("role", "user", "content", prompt));
    }

    // Gettery potrzebne do serializacji JSON
    public String getModel() {
        return model;
    }

    public List<Map<String, String>> getMessages() {
        return messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMax_tokens() {
        return max_tokens;
    }
}
