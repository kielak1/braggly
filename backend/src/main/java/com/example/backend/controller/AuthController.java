package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.example.backend.model.User.Role;

@Tag(name = "Auth Controller", description = "Zarządzanie autoryzacją użytkowników")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Logowanie klasyczne", description = "Pozwala użytkownikowi zalogować się przy użyciu nazwy użytkownika i hasła.")
    @ApiResponse(responseCode = "200", description = "Zwraca token JWT w przypadku poprawnych danych logowania.")
    @ApiResponse(responseCode = "401", description = "Niepoprawne dane logowania.", content = @Content)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        Optional<User> user = userRepository.findByUsername(loginRequest.getUsername());

        if (user.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), user.get().getPassword())) {
            String token = jwtUtil.generateToken(user.get().getUsername());
            return ResponseEntity.ok().body(Map.of("token", token));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }

    @Operation(summary = "Logowanie przez Google OAuth", description = "Uwierzytelnia użytkownika na podstawie tokena Google OAuth i zwraca token JWT.")
    @ApiResponse(responseCode = "200", description = "Zwraca token JWT po poprawnej weryfikacji tokena Google.")
    @ApiResponse(responseCode = "401", description = "Nieprawidłowy token Google.", content = @Content)
    @ApiResponse(responseCode = "500", description = "Błąd podczas weryfikacji tokena Google.", content = @Content)
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String googleToken = request.get("token");

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList("572801009620-baem7okp3f4ij8rv460l8t76evom0h5o.apps.googleusercontent.com")) // CLIENT_ID
                    .build();

            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Google Token"));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            Optional<User> userOptional = userRepository.findByUsername(email);
            User user;

            if (userOptional.isEmpty()) {
                user = new User(email, passwordEncoder.encode(UUID.randomUUID().toString()), Role.USER);
                userRepository.save(user);
            } else {
                user = userOptional.get();
            }

            String jwtToken = jwtUtil.generateToken(user.getUsername());

            return ResponseEntity.ok().body(Map.of("token", jwtToken));
        } catch (GeneralSecurityException | IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Google authentication failed"));
        }
    }
}
