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

    // @PostMapping("/create-user")
    // public ResponseEntity<String> createUser(@RequestParam String username, @RequestParam String password) {
    //     userService.createUser(username, password, User.Role.USER);
    //     return ResponseEntity.ok("User created successfully.");
    // }


    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(@RequestBody User user) {
        userService.createUser(user.getUsername(), user.getPassword(), User.Role.USER);
        return ResponseEntity.ok("User created successfully.");
    }
    

}
