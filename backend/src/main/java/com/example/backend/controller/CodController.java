package com.example.backend.controller;

import com.example.backend.dto.CodImportResult;
import com.example.backend.service.CodImportService;

import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cod")
public class CodController {

    private final CodImportService codImportService;

    public CodController(CodImportService codImportService) {
        this.codImportService = codImportService;
    }

    @PostMapping("/search")
    public List<CodImportResult> searchByElements(@RequestBody String body) {
        List<String> elements = Arrays.stream(body.trim().split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        return codImportService.importFromCod(elements);
    }
}
