package com.example.backend.repository;

import com.example.backend.model.CreditUsageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CreditUsageHistoryRepository extends JpaRepository<CreditUsageHistory, Long> {
    List<CreditUsageHistory> findByUserId(Long userId);
}