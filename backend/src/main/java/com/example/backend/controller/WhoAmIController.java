package com.example.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.backend.model.User;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WhoAmIController {

    @GetMapping("/whoami")
    public Map<String, Object> sayHello(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String username = user.getUsername();
        String role = user.getRole().name();

        return Map.of(
                "username", username,
                "role", role
        );
    }
}