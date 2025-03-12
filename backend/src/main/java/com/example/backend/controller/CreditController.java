package com.example.backend.controller;

import com.example.backend.model.CreditPackage;
import com.example.backend.model.CreditPurchaseHistory;
import com.example.backend.model.CreditUsageHistory;
import com.example.backend.service.CreditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/credits")
public class CreditController {
    
    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("/packages")
    public List<CreditPackage> getPackages() {
        return creditService.getAllCreditPackages();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/packages")
    public void addPackage(@RequestBody CreditPackage creditPackage) {
        creditService.addCreditPackage(creditPackage);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/packages/{id}")
    public void deletePackage(@PathVariable Long id) {
        creditService.deleteCreditPackage(id);
    }

    @GetMapping("/purchase-history")
    public List<CreditPurchaseHistory> getPurchaseHistory(@RequestParam Long userId) {
        return creditService.getPurchaseHistory(userId);
    }

    @GetMapping("/usage-history")
    public List<CreditUsageHistory> getUsageHistory(@RequestParam Long userId) {
        return creditService.getUsageHistory(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign")
    public ResponseEntity<?> assignCredits(@RequestParam Long userId, @RequestParam Long packageId) {
        try {
            creditService.assignCredits(userId, packageId);
            return ResponseEntity.ok("Credits assigned successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
