package com.example.backend.repository;

import com.example.backend.model.CodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CodEntryRepository extends JpaRepository<CodEntry, Long> {
    Optional<CodEntry> findByCodId(String codId);

    Optional<CodEntry> findFirstByFormula(String formula);

    List<CodEntry> findAllByFormula(String formula);

    List<CodEntry> findAllByCodIdIn(List<String> codIds);
}
