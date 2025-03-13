package com.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.backend.model.User;
import com.example.backend.model.UserCredits;
import com.example.backend.service.CreditService;

import java.util.Map;

@Tag(name = "WhoAmI Controller", description = "Zwraca informacje o aktualnie zalogowanym użytkowniku")
@RestController
@RequestMapping("/api")
public class WhoAmIController {

    private final CreditService creditService;

    public WhoAmIController(CreditService creditService) {
        this.creditService = creditService;
    }

    @Operation(
        summary = "Zwraca informacje o zalogowanym użytkowniku", 
        description = "Endpoint zwraca ID, nazwę użytkownika, rolę oraz stan kredytów aktualnie zalogowanego użytkownika."
    )
    @ApiResponse(responseCode = "200", description = "Dane użytkownika zwrócone pomyślnie", 
        content = @Content(mediaType = "application/json",
        schema = @Schema(implementation = UserInfoResponse.class)))
    @ApiResponse(responseCode = "401", description = "Nieautoryzowany dostęp", content = @Content)
    @GetMapping("/whoami")
    public Map<String, Object> whoAmI(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long id = user.getId();
        String username = user.getUsername();
        String role = user.getRole().name();

        UserCredits userCredits = user.getUserCredits();

        if (userCredits != null) {
            creditService.updateUserCreditsBalance(userCredits); // Aktualizacja salda przed zwróceniem danych
        }

        Integer balance = (userCredits != null) ? userCredits.getBalance() : null;
        String lastUpdated = (userCredits != null && userCredits.getLastUpdated() != null) 
            ? userCredits.getLastUpdated().toString() 
            : null;

        return Map.of(
                "id", id,
                "username", username,
                "role", role,
                "balance", balance,
                "lastUpdated", lastUpdated
        );
    }

    // Klasa reprezentująca odpowiedź API
    private static class UserInfoResponse {
        @Schema(description = "ID użytkownika", example = "1")
        public Long id;

        @Schema(description = "Nazwa użytkownika", example = "john_doe")
        public String username;

        @Schema(description = "Rola użytkownika", example = "ADMIN")
        public String role;

        @Schema(description = "Dostępne saldo kredytów", example = "100")
        public Integer balance;

        @Schema(description = "Data ostatniej aktualizacji salda", example = "2024-03-12T10:30:00")
        public String lastUpdated;
    }
}
