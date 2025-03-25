package com.example.backend.repository;

import com.example.backend.model.CreditPurchaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CreditPurchaseHistoryRepository extends JpaRepository<CreditPurchaseHistory, Long> {
    List<CreditPurchaseHistory> findByUserId(Long userId);
}