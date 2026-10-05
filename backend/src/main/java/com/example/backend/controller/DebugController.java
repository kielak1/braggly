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
        return environmentVariables(System.getenv());
    }

    Map<String, String> environmentVariables(Map<String, String> environment) {
        Map<String, String> filtered = new LinkedHashMap<>();
        environment.forEach((key, value) -> {
            if (key.startsWith("SERVER_") || key.startsWith("DATABASE_URL")) {
                filtered.put(key, "configured: " + (value != null && !value.isBlank()));
            }
        });
        return filtered;
    }

    @GetMapping("/time")
    public String currentTime() {
        return LocalDateTime.now().toString();
    }
}
