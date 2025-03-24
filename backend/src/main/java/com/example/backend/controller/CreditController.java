package com.example.backend.controller;

import com.example.backend.model.CreditPackage;
import com.example.backend.model.CreditPurchaseHistory;
import com.example.backend.model.CreditUsageHistory;
import com.example.backend.service.CreditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Credit Controller", description = "Zarządzanie pakietami kredytowymi oraz historią zakupów i użycia kredytów")
@RestController
@RequestMapping("/credits")
public class CreditController {
    
    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @Operation(summary = "Pobiera dostępne pakiety kredytowe", description = "Zwraca listę wszystkich pakietów kredytowych dostępnych w systemie.")
    @ApiResponse(responseCode = "200", description = "Lista pakietów kredytowych zwrócona pomyślnie")
    @GetMapping("/packages")
    public List<CreditPackage> getPackages() {
        return creditService.getAllCreditPackages();
    }

    @Operation(summary = "Dodaje nowy pakiet kredytowy", description = "Dostępne tylko dla administratorów.")
    @ApiResponse(responseCode = "200", description = "Pakiet kredytowy dodany pomyślnie")
    @ApiResponse(responseCode = "403", description = "Brak uprawnień do dodawania pakietów", content = @Content)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/packages")
    public void addPackage(@RequestBody CreditPackage creditPackage) {
        creditService.addCreditPackage(creditPackage);
    }

    @Operation(summary = "Usuwa pakiet kredytowy", description = "Dostępne tylko dla administratorów.")
    @ApiResponse(responseCode = "200", description = "Pakiet kredytowy usunięty pomyślnie")
    @ApiResponse(responseCode = "403", description = "Brak uprawnień do usuwania pakietów", content = @Content)
    @ApiResponse(responseCode = "404", description = "Pakiet kredytowy nie znaleziony", content = @Content)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/packages/{id}")
    public void deletePackage(@PathVariable Long id) {
        creditService.deleteCreditPackage(id);
    }

    @Operation(summary = "Pobiera historię zakupów kredytów", description = "Zwraca historię zakupów użytkownika na podstawie jego ID.")
    @ApiResponse(responseCode = "200", description = "Historia zakupów kredytowych zwrócona pomyślnie")
    @ApiResponse(responseCode = "400", description = "Nieprawidłowy identyfikator użytkownika", content = @Content)
    @GetMapping("/purchase-history")
    public List<CreditPurchaseHistory> getPurchaseHistory(@RequestParam Long userId) {
        return creditService.getPurchaseHistory(userId);
    }

    @Operation(summary = "Pobiera historię użycia kredytów", description = "Zwraca historię wykorzystania kredytów przez użytkownika na podstawie jego ID.")
    @ApiResponse(responseCode = "200", description = "Historia użycia kredytów zwrócona pomyślnie")
    @ApiResponse(responseCode = "400", description = "Nieprawidłowy identyfikator użytkownika", content = @Content)
    @GetMapping("/usage-history")
    public List<CreditUsageHistory> getUsageHistory(@RequestParam Long userId) {
        return creditService.getUsageHistory(userId);
    }

    @Operation(summary = "Przypisuje kredyty do użytkownika", description = "Administrator może przypisać pakiet kredytowy do użytkownika.")
    @ApiResponse(responseCode = "200", description = "Kredyty przypisane pomyślnie")
    @ApiResponse(responseCode = "400", description = "Nieprawidłowe dane wejściowe", content = @Content)
    @ApiResponse(responseCode = "403", description = "Brak uprawnień do przypisywania kredytów", content = @Content)
    @ApiResponse(responseCode = "500", description = "Błąd podczas przypisywania kredytów", content = @Content)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign")
    public ResponseEntity<?> assignCredits(@RequestParam Long userId, @RequestParam Long packageId) {
        try {
            creditService.assignCredits(userId, packageId, "");
            return ResponseEntity.ok("Credits assigned successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
