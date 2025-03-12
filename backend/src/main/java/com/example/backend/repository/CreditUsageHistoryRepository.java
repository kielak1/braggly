package com.example.backend.repository;

import com.example.backend.model.CreditUsageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditUsageHistoryRepository extends JpaRepository<CreditUsageHistory, Long> {}


