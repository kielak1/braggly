package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
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

    /**
     * Logowanie za pomocą klasycznego loginu i hasła
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        Optional<User> user = userRepository.findByUsername(loginRequest.getUsername());

        if (user.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), user.get().getPassword())) {
            String token = jwtUtil.generateToken(user.get().getUsername());
            return ResponseEntity.ok().body(Map.of("token", token));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }

    /**
     * Logowanie za pomocą Google OAuth
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String googleToken = request.get("token");

        try {
            // Weryfikator tokena Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                    .setAudience(Collections
                            .singletonList("572801009620-baem7okp3f4ij8rv460l8t76evom0h5o.apps.googleusercontent.com")) // <--
                                                                                                                        // Twój
                                                                                                                        // CLIENT_ID
                    .build();

            // Weryfikacja tokena
            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Google Token"));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // Sprawdzenie użytkownika w bazie
            Optional<User> userOptional = userRepository.findByUsername(email);
            User user;

            if (userOptional.isEmpty()) {
                user = new User(email, passwordEncoder.encode(UUID.randomUUID().toString()), Role.USER);
                userRepository.save(user);

                // user = new User();
                // user.setUsername(email);
                // user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); //
                // Generowanie losowego hasła
                // user.setRole(Role.USER);
                // userRepository.save(user);
            } else {
                user = userOptional.get();
            }

            // Generowanie JWT
            String jwtToken = jwtUtil.generateToken(user.getUsername());

            return ResponseEntity.ok().body(Map.of("token", jwtToken));
        } catch (GeneralSecurityException | IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Google authentication failed"));
        }
    }
}
