package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(@RequestBody Map<String, String> userData, Authentication authentication) {
        String username = userData.get("username");
        String password = userData.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("Missing username or password.");
        }

        User user = (User) authentication.getPrincipal();
        String role = user.getRole().name();

        if (role != "ADMIN" ) {
            return ResponseEntity.badRequest().body("Brak uprawnień.");
        }

        userService.createUser(username, password, User.Role.USER);
        return ResponseEntity.ok("User created successfully.");
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<String> deleteUser(@RequestParam String username) {
        boolean deleted = userService.deleteUser(username);
        if (deleted) {
            return ResponseEntity.ok("User deleted successfully.");
        } else {
            return ResponseEntity.badRequest().body("User not found.");
        }
    }
}
