package com.example.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.service.OpenAiService;

import java.util.Map;

@RestController
@RequestMapping("/openai")
public class OpenAiController {

    private final OpenAiService openAiService;

    public OpenAiController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping("/simple")
    public ResponseEntity<String> getOpenAiResponse(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body("Brak treści zapytania.");
        }
        String response = openAiService.askOpenAi(prompt);
        return ResponseEntity.ok(response);
    }
}
