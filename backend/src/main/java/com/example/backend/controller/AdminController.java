package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.HashMap;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;


import org.springframework.security.crypto.password.PasswordEncoder;

@Tag(name = "Admin Controller", description = "Zarządzanie użytkownikami w systemie")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "Tworzy nowego użytkownika", description = "Endpoint dostępny tylko dla administratorów.")
    @ApiResponse(responseCode = "200", description = "Pomyślnie utworzono użytkownika")
    @ApiResponse(responseCode = "400", description = "Brak nazwy użytkownika lub hasła", content = @Content)
    @ApiResponse(responseCode = "403", description = "Brak uprawnień do utworzenia użytkownika", content = @Content)
    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(@RequestBody Map<String, String> userData, Authentication authentication) {
        String username = userData.get("username");
        String password = userData.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("Missing username or password.");
        }

        User user = (User) authentication.getPrincipal();
        String role = user.getRole().name();

        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Brak uprawnień.");
        }

        userService.createUser(username, password, User.Role.USER);
        return ResponseEntity.ok("User created successfully.");
    }

    @Operation(summary = "Usuwa użytkownika", description = "Endpoint dostępny tylko dla administratorów. Wymaga podania nazwy użytkownika do usunięcia.")
    @ApiResponse(responseCode = "200", description = "Pomyślnie usunięto użytkownika")
    @ApiResponse(responseCode = "400", description = "Użytkownik nie znaleziony", content = @Content)
    @ApiResponse(responseCode = "403", description = "Brak uprawnień do usunięcia użytkownika", content = @Content)
    @DeleteMapping("/delete-user")
    public ResponseEntity<String> deleteUser(@RequestParam String username, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String role = user.getRole().name();

        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Brak uprawnień.");
        }
        boolean deleted = userService.deleteUser(username);
        if (deleted) {
            return ResponseEntity.ok("User deleted successfully.");
        } else {
            return ResponseEntity.badRequest().body("User not found.");
        }
    }

    @Operation(summary = "Pobiera listę użytkowników", description = "Zwraca listę wszystkich użytkowników wraz z ich rolami oraz saldem kredytowym. Dostępne tylko dla administratorów.")
    @ApiResponse(responseCode = "200", description = "Lista użytkowników zwrócona pomyślnie")
    @ApiResponse(responseCode = "403", description = "Brak uprawnień do wyświetlenia listy użytkowników", content = @Content)
    @GetMapping("/list-user")
    public ResponseEntity<List<Map<String, Object>>> listUsers(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<User> users = userService.getAllUsers();
        List<Map<String, Object>> userList = users.stream().map(u -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", u.getId()); // Long
            userData.put("username", u.getUsername()); // String
            userData.put("role", u.getRole().name()); // String
            userData.put("balance", u.getUserCredits() != null ? u.getUserCredits().getBalance() : 0); // Integer

            return userData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(userList);
    }

    @Operation(summary = "Zmienia rolę użytkownika", description = "Pozwala administratorowi zmienić rolę użytkownika na ADMIN lub USER")
    @ApiResponse(responseCode = "200", description = "Rola użytkownika została zmieniona")
    @ApiResponse(responseCode = "403", description = "Brak uprawnień")
    @ApiResponse(responseCode = "404", description = "Użytkownik nie znaleziony")
    @PutMapping("/set-role")
    public ResponseEntity<String> setUserRole(
            @RequestParam Long userId,
            @RequestParam String role,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Brak uprawnień");
        }

        Optional<User> userOptional = userService.getUserById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Użytkownik nie znaleziony");
        }

        try {
            User.Role newRole = User.Role.valueOf(role.toUpperCase());
            userService.updateUserRole(userId, newRole);
            return ResponseEntity.ok("Rola użytkownika zmieniona na: " + newRole);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Niepoprawna rola: " + role);
        }
    }

    @Operation(summary = "Zmienia hasło użytkownika", description = "Pozwala administratorowi zmienić hasło użytkownika")
    @ApiResponse(responseCode = "200", description = "Hasło zostało zmienione")
    @ApiResponse(responseCode = "403", description = "Brak uprawnień")
    @ApiResponse(responseCode = "404", description = "Użytkownik nie znaleziony")
    @PutMapping("/set-password")
    public ResponseEntity<String> setUserPassword(
            @RequestParam Long userId,
            @RequestParam String newPassword,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Brak uprawnień");
        }

        Optional<User> userOptional = userService.getUserById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Użytkownik nie znaleziony");
        }

        userService.updateUserPassword(userId, newPassword);
        return ResponseEntity.ok("Hasło użytkownika zostało zmienione");
    }

}
