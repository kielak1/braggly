package com.example.backend.repository;

import com.example.backend.model.CodQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CodQueryRepository extends JpaRepository<CodQuery, Long> {

    @Query("SELECT q FROM CodQuery q WHERE q.requestedAt > :cutoff AND q.completed = true")
    List<CodQuery> findRecentCompletedQueries(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT q FROM CodQuery q WHERE q.requestedAt > :cutoff AND q.status IN (com.example.backend.model.CodQueryStatus.PENDING, com.example.backend.model.CodQueryStatus.RUNNING)")
    List<CodQuery> findRecentPendingQueries(@Param("cutoff") LocalDateTime cutoff);

    Optional<CodQuery> findFirstByElementSetAndRequestedAtAfterOrderByRequestedAtDesc(
            String elementSet, LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("UPDATE CodQuery q SET q.status = com.example.backend.model.CodQueryStatus.FAILED, q.completed = false WHERE q.status IN (com.example.backend.model.CodQueryStatus.PENDING, com.example.backend.model.CodQueryStatus.RUNNING)")
    int failInterruptedQueries();

}
