package com.example.backend.service;

import com.example.backend.model.*;
import com.example.backend.repository.*;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CreditService {
    private final UserRepository userRepository;
    private final CreditPackageRepository creditPackageRepository;
    private final UserCreditsRepository userCreditsRepository;
    private final CreditPurchaseHistoryRepository creditPurchaseHistoryRepository;
    private final CreditUsageHistoryRepository creditUsageHistoryRepository;
    private static final Logger logger = LoggerFactory.getLogger(CreditService.class);

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
    public void purchaseCredits(Long userId, Long packageId, String paymentId) {
        // purchaseCredits bezpośrednio dodaje kredyty do salda użytkownika
        // i zapisuje historię zakupu.
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
        userCredits.setLastUpdated(LocalDateTime.now()); // Aktualizacja lastUpdated
        userCreditsRepository.save(userCredits);

        CreditPurchaseHistory history = new CreditPurchaseHistory();
        history.setUserId(userId);
        history.setCreditsPurchased(creditPackage.getCredits());
        history.setAmountPaid(creditPackage.getPriceInCents());
        history.setPurchaseDate(LocalDateTime.now());

        history.setPaymentId(paymentId);

        creditPurchaseHistoryRepository.save(history);
    }

    @Transactional
    public void useCredits(Long userId, String usageType, int creditsUsed) {
        if (creditsUsed == 0) {
            return;
        }

        UserCredits userCredits = userCreditsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userCredits.getBalance() < creditsUsed) {
            throw new RuntimeException("Insufficient balance");
        }

        userCredits.setBalance(userCredits.getBalance() - creditsUsed);
        userCredits.setLastUpdated(LocalDateTime.now());

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
    public ResponseEntity<?> assignCredits(Long userId, Long packageId, String paymentId) {
        // assignCredits najpierw sprawdza i aktualizuje saldo kredytów użytkownika
        // jeśli data lastUpdated jest starsza, a następnie wywołuje purchaseCredits,
        // aby dodać kredyty.
        UserCredits userCredits = userCreditsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserCredits newCredits = new UserCredits();
                    newCredits.setUserId(userId);
                    newCredits.setBalance(0);
                    return userCreditsRepository.save(newCredits);
                });

        updateUserCreditsBalance(userCredits);

        // Wywołujemy purchaseCredits, aby uniknąć duplikacji logiki
        purchaseCredits(userId, packageId, paymentId);
        return ResponseEntity.ok("Credits assigned successfully");
    }

    @Transactional
    public void updateUserCreditsBalance(UserCredits userCredits) {
        if (userCredits.getLastUpdated() != null) {
            LocalDate lastUpdatedDate = userCredits.getLastUpdated().toLocalDate();
            LocalDate today = LocalDate.now();
            long daysBetween = ChronoUnit.DAYS.between(lastUpdatedDate, today);

            // Logowanie wartości daysBetween
            // logger.info("User ID: {}, daysBetween: {}, lastUpdatedDate: {}, today: {},
            // currentBalance: {}",
            // userCredits.getUserId(), daysBetween, lastUpdatedDate, today,
            // userCredits.getBalance());

            if (daysBetween > 0) {
                // Pobieramy balans przed odjęciem kredytów
                int currentBalance = userCredits.getBalance();

                // Kredyty, które użytkownik faktycznie straci
                int creditsUsed = (int) Math.min(daysBetween, currentBalance);

                // Odejmujemy kredyty za pomocą useCredits()
                useCredits(userCredits.getUserId(), "time", creditsUsed);

                // Aktualizujemy saldo (unikamy podwójnego odejmowania)
                int newBalance = Math.max(currentBalance - (int) daysBetween, 0);
                userCredits.setBalance(newBalance);
                userCredits.setLastUpdated(LocalDateTime.now());

                // Zapis do bazy
                userCreditsRepository.save(userCredits);

                // Logowanie nowego balansu
                // logger.info("User ID: {}, creditsUsed: {}, previousBalance: {}, newBalance:
                // {}, lastUpdated updated to: {}",
                // userCredits.getUserId(), creditsUsed, currentBalance, newBalance,
                // userCredits.getLastUpdated());
            }
        }
    }

    @Transactional
    public void deleteCreditPackage(Long id) {
        if (!creditPackageRepository.existsById(id)) {
            throw new RuntimeException("Credit package not found");
        }
        creditPackageRepository.deleteById(id);
    }

}
