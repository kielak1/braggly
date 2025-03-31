package com.example.backend.controller;

import com.example.backend.model.RestrictedPath;
import com.example.backend.service.RestrictedPathService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parameters/restricted-paths")
public class RestrictedPathController {

    private final RestrictedPathService restrictedPathService;

    public RestrictedPathController(RestrictedPathService restrictedPathService) {
        this.restrictedPathService = restrictedPathService;
    }

    @GetMapping
    @Operation(summary = "Zwraca listę płatnych ścieżek w systemie")
    @PreAuthorize("isAuthenticated()")
    public List<RestrictedPath> getAll() {
        return restrictedPathService.getAllRestrictedPaths();
    }

    @PostMapping
    @Operation(summary = "Dodaje nową płatną ścieżkę")
    @PreAuthorize("hasRole('ADMIN')")
    public RestrictedPath addPath(@RequestParam String path) {
        return restrictedPathService.addPath(path);
    }

    @DeleteMapping
    @Operation(summary = "Usuwa płatną ścieżkę")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePath(@RequestParam String path) {
        restrictedPathService.deletePath(path);
    }
}
