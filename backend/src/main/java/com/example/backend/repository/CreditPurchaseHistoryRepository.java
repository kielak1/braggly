package com.example.backend.repository;

import com.example.backend.model.CreditPurchaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditPurchaseHistoryRepository extends JpaRepository<CreditPurchaseHistory, Long> {}

