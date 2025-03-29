package com.example.backend.controller;

import com.example.backend.model.ParametersBool;
import com.example.backend.model.User;
import com.example.backend.service.ParametersBoolService;

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
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag(name = "Parameters Controller", description = "Zarządzanie parametrami typu boolean w systemie")
@RestController
@RequestMapping("/parameters")
public class ParametersController {

    private static final Logger logger = LoggerFactory.getLogger(ParametersController.class);
    private final ParametersBoolService parametersBoolService;

    public ParametersController(ParametersBoolService parametersBoolService) {
        this.parametersBoolService = parametersBoolService;
    }

    @Operation(summary = "Usuwa parametr", description = "Endpoint dostępny tylko dla administratorów. Wymaga podania nazwy parametru do usunięcia.")
    @ApiResponse(responseCode = "200", description = "Pomyślnie usunięto parametr")
    @ApiResponse(responseCode = "400", description = "Parametr nie znaleziony", content = @Content)
    @ApiResponse(responseCode = "403", description = "Brak uprawnień do usunięcia parametru", content = @Content)
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteParameter(
            @RequestParam String name,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        String role = user.getRole().name();

        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Brak uprawnień.");
        }

        boolean deleted = parametersBoolService.deleteParameter(name);
        if (deleted) {
            return ResponseEntity.ok("Parametr usunięty pomyślnie.");
        } else {
            return ResponseEntity.badRequest().body("Parametr nie znaleziony.");
        }
    }

    @Operation(summary = "Pobiera listę parametrów", description = "Zwraca listę wszystkich parametrów typu boolean. Dostępne tylko dla administratorów.")
    @ApiResponse(responseCode = "200", description = "Lista parametrów zwrócona pomyślnie")
    @ApiResponse(responseCode = "403", description = "Brak uprawnień do wyświetlenia listy parametrów", content = @Content)
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listParameters(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (!user.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ParametersBool> parameters = parametersBoolService.getAllParameters();
        List<Map<String, Object>> parameterList = parameters.stream().map(p -> {
            Map<String, Object> paramData = new HashMap<>();
            paramData.put("id", p.getId()); // Long
            paramData.put("name", p.getName()); // String
            paramData.put("value", p.getValue()); // Boolean
            return paramData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(parameterList);
    }

    @Operation(summary = "Aktualizuje wartość parametru", description = "Pozwala administratorowi zmienić wartość parametru")
    @ApiResponse(responseCode = "200", description = "Wartość parametru została zmieniona")
    @ApiResponse(responseCode = "403", description = "Brak uprawnień")
    @ApiResponse(responseCode = "404", description = "Parametr nie znaleziony")
    @PutMapping("/update")
    public ResponseEntity<String> updateParameter(
            @RequestParam String name,
            @RequestParam Boolean value,
            Authentication authentication) {
        User adminUser = (User) authentication.getPrincipal();
        String role = adminUser.getRole().name();

        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Brak uprawnień.");
        }

        Optional<ParametersBool> paramOptional = parametersBoolService.findByName(name);
        if (paramOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Parametr nie znaleziony");
        }

        parametersBoolService.updateParameter(name, value);
        return ResponseEntity.ok("Wartość parametru zmieniona na: " + value);
    }

    @Operation(summary = "Pobiera wartość parametru", description = "Zwraca wartość parametru typu boolean. Dostępne dla wszystkich użytkowników.")
    @ApiResponse(responseCode = "200", description = "Wartość parametru została pobrana")
    @ApiResponse(responseCode = "404", description = "Parametr nie znaleziony")
    @GetMapping("/get")
    public ResponseEntity<String> getParameter(@RequestParam String name) {
        Optional<ParametersBool> paramOptional = parametersBoolService.findByName(name);
        if (paramOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Parametr nie znaleziony");
        }
        Boolean result = parametersBoolService.getParameterValue(name);
        return ResponseEntity.ok(result.toString());
    }
}