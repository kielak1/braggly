package com.example.backend.controller;

import com.example.backend.dto.CodImportResult;
import com.example.backend.dto.CodQueryStatusResponse;
import com.example.backend.model.CodEntry;
import com.example.backend.service.CodImportService;
import com.example.backend.repository.CodEntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional; // ✅ IMPORT DODANY
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cod")
public class CodController {

    private final CodImportService codImportService;
    private final CodEntryRepository codEntryRepository;

    public CodController(CodImportService codImportService, CodEntryRepository codEntryRepository) {
        this.codImportService = codImportService;
        this.codEntryRepository = codEntryRepository;
    }

    @PostMapping("/search")
    public CodQueryStatusResponse searchOrPoll(@RequestBody String body) {
        List<String> elements = Arrays.stream(body.trim().split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        return codImportService.checkAndImport(elements);
    }

    @GetMapping("/id")
    public ResponseEntity<List<String>> getCodIdsByFormula(@RequestParam String formula) {
        List<CodEntry> entries = codEntryRepository.findAllByFormula(formula);
        if (entries.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<String> codIds = entries.stream()
                .map(CodEntry::getCodId)
                .collect(Collectors.toList());
        return ResponseEntity.ok(codIds);
    }

}
