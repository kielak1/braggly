package com.example.backend.repository;

import com.example.backend.model.ParametersBool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametersBoolRepository extends JpaRepository<ParametersBool, Long> {
    boolean existsByName(String name);
    ParametersBool findByName(String name);
}