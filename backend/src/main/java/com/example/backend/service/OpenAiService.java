package com.example.backend.service;

import com.example.backend.dto.OpenAiRequest;
import com.example.backend.dto.OpenAiResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OpenAiService {

    private final WebClient webClient;

    public OpenAiService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + System.getenv("OPEN_AI_KEY"))
                .build();
    }

    public String askOpenAi(String prompt) {
        OpenAiRequest request = new OpenAiRequest(prompt);
        OpenAiResponse response = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        if (response != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }
        return "Brak odpowiedzi od OpenAI.";
    }
}
