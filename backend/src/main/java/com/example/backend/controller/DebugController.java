package com.example.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping("/java")
    public String javaVersion() {
        return System.getProperty("java.version");
    }

    @GetMapping("/env")
    public Map<String, String> environmentVariables() {
        Map<String, String> filtered = new LinkedHashMap<>();
        System.getenv().forEach((key, value) -> {
            if (key.startsWith("SERVER_") || key.startsWith("DATABASE_URL")) {
                filtered.put(key, value);
            }
            // if (key.startsWith("SERVER_") || key.startsWith("DATABASE_") ||
            // key.startsWith("STRIPE_") || key.startsWith("JWT") ||
            // key.startsWith("GOOGLE_")) {
            // filtered.put(key, value);
            // }
        });
        return filtered;
    }

    @GetMapping("/time")
    public String currentTime() {
        return LocalDateTime.now().toString();
    }
}
