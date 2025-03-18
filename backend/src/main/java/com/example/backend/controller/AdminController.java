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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.context.SecurityContextHolder;

@Tag(name = "Admin Controller", description = "Zarządzanie użytkownikami w systemie")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
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

}
