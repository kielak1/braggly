package com.example.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // 🔓 Pozwala na dostęp do wszystkich endpointów
            )
            .csrf(csrf -> csrf.disable()); // ⚠ Wyłącza CSRF dla REST API (opcjonalnie)
        return http.build();
    }
}
