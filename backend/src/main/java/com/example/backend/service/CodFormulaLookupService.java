package com.example.backend.service;

import com.example.backend.repository.CodEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CodFormulaLookupService {
    private final CodEntryRepository repository;

    public CodFormulaLookupService(CodEntryRepository repository) {
        this.repository = repository;
    }

    public List<String> findCodIds(String formula) {
        var requested = ChemicalComposition.parse(formula);
        if (requested.isEmpty()) {
            // Preserve existing exact lookups for notation outside the flat-sum contract.
            return repository.findAllByFormula(formula).stream().map(entry -> entry.getCodId()).toList();
        }
        String pattern = ChemicalComposition.candidatePattern(requested.get().keySet());
        List<String> ids = new ArrayList<>();
        long afterId = 0;
        while (true) {
            var candidates = repository.findFormulaCandidates(pattern, afterId, PageRequest.of(0, 500));
            for (var candidate : candidates) {
                if (ChemicalComposition.parse(candidate.getFormula()).filter(requested.get()::equals).isPresent()) {
                    ids.add(candidate.getCodId());
                }
                afterId = candidate.getId();
            }
            if (candidates.size() < 500) return ids;
        }
    }
}
