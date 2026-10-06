package com.example.backend.repository;

import com.example.backend.model.CodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodEntryRepository extends JpaRepository<CodEntry, Long> {
    Optional<CodEntry> findByCodId(String codId);

    Optional<CodEntry> findFirstByFormula(String formula);

    List<CodEntry> findAllByFormula(String formula);

    interface FormulaCandidate {
        Long getId();
        String getCodId();
        String getFormula();
    }

    @Query(value = "SELECT id, cod_id AS \"codId\", formula FROM cod_entry "
            + "WHERE id > :afterId AND formula ~ :pattern ORDER BY id", nativeQuery = true)
    List<FormulaCandidate> findFormulaCandidates(@Param("pattern") String pattern,
            @Param("afterId") long afterId, Pageable pageable);

    List<CodEntry> findAllByCodIdIn(List<String> codIds);
}
