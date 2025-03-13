package com.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Hello Controller", description = "Prosty kontroler zwracający powitanie")
@RestController
@RequestMapping("/api")
public class HelloController {

    @Operation(summary = "Zwraca powitanie", description = "Prosty endpoint zwracający wiadomość 'Hello, world!'")
    @ApiResponse(responseCode = "200", description = "Powitanie zwrócone pomyślnie")
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, world!";
    }
}
