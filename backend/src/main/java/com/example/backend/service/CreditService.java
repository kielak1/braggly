package com.example.backend.service;

import com.example.backend.model.*;
import com.example.backend.repository.*;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;

@Service
public class CreditService {
    private final UserRepository userRepository;
    private final CreditPackageRepository creditPackageRepository;
    private final UserCreditsRepository userCreditsRepository;
    private final CreditPurchaseHistoryRepository creditPurchaseHistoryRepository;
    private final CreditUsageHistoryRepository creditUsageHistoryRepository;

    public CreditService(UserRepository userRepository,
            CreditPackageRepository creditPackageRepository,
            UserCreditsRepository userCreditsRepository,
            CreditPurchaseHistoryRepository creditPurchaseHistoryRepository,
            CreditUsageHistoryRepository creditUsageHistoryRepository) {
        this.userRepository = userRepository;
        this.creditPackageRepository = creditPackageRepository;
        this.userCreditsRepository = userCreditsRepository;
        this.creditPurchaseHistoryRepository = creditPurchaseHistoryRepository;
        this.creditUsageHistoryRepository = creditUsageHistoryRepository;
    }

    public List<CreditPackage> getAllCreditPackages() {
        return creditPackageRepository.findAll();
    }

    @Transactional
    public void purchaseCredits(Long userId, Long packageId) {
        CreditPackage creditPackage = creditPackageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Credit package not found"));

        UserCredits userCredits = userCreditsRepository.findById(userId)
                .orElseGet(() -> {
                    UserCredits newUserCredits = new UserCredits();
                    newUserCredits.setUserId(userId);
                    newUserCredits.setBalance(0);
                    return userCreditsRepository.save(newUserCredits);
                });

        userCredits.setBalance(userCredits.getBalance() + creditPackage.getCredits());
        userCreditsRepository.save(userCredits);

        CreditPurchaseHistory history = new CreditPurchaseHistory();
        history.setUserId(userId);
        history.setCreditsPurchased(creditPackage.getCredits());
        history.setAmountPaid(creditPackage.getPriceInCents());
        history.setPurchaseDate(LocalDateTime.now());
        creditPurchaseHistoryRepository.save(history);
    }

    @Transactional
    public void useCredits(Long userId, String usageType, int creditsUsed) {
        UserCredits userCredits = userCreditsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userCredits.getBalance() < creditsUsed) {
            throw new RuntimeException("Insufficient balance");
        }

        userCredits.setBalance(userCredits.getBalance() - creditsUsed);
        userCreditsRepository.save(userCredits);

        CreditUsageHistory usageHistory = new CreditUsageHistory();
        usageHistory.setUserId(userId);
        usageHistory.setUsageType(usageType);
        usageHistory.setUsageDate(LocalDateTime.now());
        usageHistory.setCreditsUsed(creditsUsed);
        creditUsageHistoryRepository.save(usageHistory);
    }

    public List<CreditPurchaseHistory> getPurchaseHistory(Long userId) {
        return creditPurchaseHistoryRepository.findAll();
    }

    public List<CreditUsageHistory> getUsageHistory(Long userId) {
        return creditUsageHistoryRepository.findAll();
    }

    @Transactional
    public void addCreditPackage(CreditPackage creditPackage) {
        creditPackageRepository.save(creditPackage);
    }

    @Transactional
    public ResponseEntity<?> assignCredits(Long userId, Long packageId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        CreditPackage creditPackage = creditPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit package not found"));

        // Pobierz wpis UserCredits lub stwórz nowy, jeśli nie istnieje
        UserCredits userCredits = userCreditsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserCredits newCredits = new UserCredits();
                    newCredits.setUser(user);
                    newCredits.setBalance(0); // Startowy stan
                    return userCreditsRepository.save(newCredits); // Zapis nowego wpisu
                });

        // Zaktualizuj saldo
        userCredits.setBalance(userCredits.getBalance() + creditPackage.getCredits());

        // Zapisz aktualizację
        userCreditsRepository.save(userCredits);

        return ResponseEntity.ok("Credits assigned successfully");
    }

    @Transactional
    public void deleteCreditPackage(Long id) {
        if (!creditPackageRepository.existsById(id)) {
            throw new RuntimeException("Credit package not found");
        }
        creditPackageRepository.deleteById(id);
    }

}
