package com.example.backend.repository;

import com.example.backend.model.UserCredits;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserCreditsRepository extends JpaRepository<UserCredits, Long> {
    Optional<UserCredits> findByUserId(Long userId);
}
