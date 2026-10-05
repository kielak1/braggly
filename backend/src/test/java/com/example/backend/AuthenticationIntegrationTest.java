package com.example.backend;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.junit.jupiter.api.Order;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthenticationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;

    @BeforeAll
    void setup() {
        // 1. Pobranie tokena
        Map<String, String> request = new HashMap<>();
        request.put("username", "admin");
        request.put("password", "admin");

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login", request, Map.class);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("token"));

        token = (String) response.getBody().get("token");
        assertNotNull(token);
    }

    @Test
    @Order(1)
    void testHelloEndpoint() {
        // 2. Użycie tokena do wywołania /api/hello
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("/api/hello", HttpMethod.GET, request, String.class);

        System.out.println("🔍 Odpowiedź z /api/hello: " + response.getBody());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(2)
    void testCreateUserEndpointWithRestTemplate() {
        // 1. Przygotowanie nagłówków
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // 2. Przygotowanie żądania (body z użytkownikiem)
        String requestBody = "{\"username\": \"testuser\", \"password\": \"testpass\"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            // 3. Wysłanie żądania do /api/admin/create-user
            ResponseEntity<String> response = restTemplate.exchange("/api/admin/create-user", HttpMethod.POST, request, String.class);

            // 4. Debugowanie wyniku
            System.out.println("📡 Otrzymany status: " + response.getStatusCode());
            System.out.println("📡 Otrzymana odpowiedź: " + response.getBody());

            // 5. Sprawdzenie odpowiedzi
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("User created successfully.", response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            fail("❌ Wystąpił błąd: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    void testDeleteUserEndpoint() {
        // 1. Przygotowanie nagłówków
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        // 2. Wysłanie żądania DELETE do /api/admin/delete-user
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("/api/admin/delete-user?username=testuser", HttpMethod.DELETE, request, String.class);

        // 3. Debugowanie wyniku
        System.out.println("📡 Otrzymany status: " + response.getStatusCode());
        System.out.println("📡 Otrzymana odpowiedź: " + response.getBody());

        // 4. Sprawdzenie odpowiedzi
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User deleted successfully.", response.getBody());
    }

    @Test
    @Order(4)
    void testDeleteNonExistingUser() {
        // 1. Przygotowanie nagłówków
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        // 2. Wysłanie żądania DELETE do /api/admin/delete-user dla nieistniejącego użytkownika
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("/api/admin/delete-user?username=nonexistent", HttpMethod.DELETE, request, String.class);

        // 3. Debugowanie wyniku
        System.out.println("📡 Otrzymany status: " + response.getStatusCode());
        System.out.println("📡 Otrzymana odpowiedź: " + response.getBody());

        // 4. Sprawdzenie odpowiedzi
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User not found.", response.getBody());
    }
}
