package com.example.backend.controller;

import com.example.backend.dto.CodImportResult;
import com.example.backend.dto.CodQueryStatusResponse;
import com.example.backend.service.CodFormulaLookupService;
import com.example.backend.model.CodQuery;
import com.example.backend.service.CodImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.backend.repository.CodQueryRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional; // ✅ IMPORT DODANY
import java.util.stream.Collectors;
import com.example.backend.dto.CodQueryShortDTO;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/cod")
public class CodController {

    private final CodImportService codImportService;
    private final CodFormulaLookupService formulaLookup;
    private final CodQueryRepository codQueryRepository;

    public CodController(
            CodImportService codImportService,
            CodFormulaLookupService formulaLookup,
            CodQueryRepository codQueryRepository) {
        this.codImportService = codImportService;
        this.formulaLookup = formulaLookup;
        this.codQueryRepository = codQueryRepository;
    }

    @PostMapping("/search")
    public CodQueryStatusResponse searchOrPoll(@RequestBody String body,
            @RequestParam(defaultValue = "false") boolean retry) {
        List<String> elements = Arrays.stream(body.trim().split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        return codImportService.checkAndImport(elements, retry);
    }

    @GetMapping("/id")
    public ResponseEntity<List<String>> getCodIdsByFormula(@RequestParam String formula) {
        return ResponseEntity.ok(formulaLookup.findCodIds(formula));
    }

    @GetMapping("/active-imports")
    public ResponseEntity<List<CodQueryShortDTO>> getActiveImports() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2400);

        List<CodQueryShortDTO> list = codQueryRepository.findRecentPendingQueries(cutoff)
                .stream()
                .map(q -> {
                    String formula = q.getElementsAsFormula();
                    String requestedAtStr = q.getRequestedAt().toString();
                    String eta = estimateRemainingTime(q.getRequestedAt(), q.getProgress());
                    return new CodQueryShortDTO(formula, requestedAtStr, eta);
                })
                .toList();

        return ResponseEntity.ok(list);
    }

    private String estimateRemainingTime(LocalDateTime requestedAt, int progress) {
        if (progress <= 0 || progress >= 100) {
            return "00:00:00";
        }

        long elapsedSeconds = java.time.Duration.between(requestedAt, LocalDateTime.now()).getSeconds();
        double estimatedTotalSeconds = elapsedSeconds / (progress / 100.0);
        long remainingSeconds = Math.round(estimatedTotalSeconds - elapsedSeconds);

        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        long seconds = remainingSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
