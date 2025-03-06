package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(@RequestParam String username, @RequestParam String password) {
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
