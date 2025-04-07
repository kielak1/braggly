package com.example.backend.repository;

import com.example.backend.model.CodQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CodQueryRepository extends JpaRepository<CodQuery, Long> {

    @Query("SELECT q FROM CodQuery q WHERE q.requestedAt > :cutoff AND q.completed = true")
    List<CodQuery> findRecentCompletedQueries(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT q FROM CodQuery q WHERE q.requestedAt > :cutoff AND q.completed = false")
    List<CodQuery> findRecentPendingQueries(@Param("cutoff") LocalDateTime cutoff);

}
