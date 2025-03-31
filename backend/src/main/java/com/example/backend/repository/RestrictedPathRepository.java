package com.example.backend.repository;

import com.example.backend.model.RestrictedPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestrictedPathRepository extends JpaRepository<RestrictedPath, Long> {
    boolean existsByPath(String path);
}
