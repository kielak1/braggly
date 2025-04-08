package com.example.backend.controller;

import com.example.backend.service.CifInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cod/cif")
public class CodCifController {

    private final CifInfoService cifInfoService;

    public CodCifController(CifInfoService cifInfoService) {
        this.cifInfoService = cifInfoService;
    }

    @GetMapping("/{codId}")
    public ResponseEntity<Map<String, String>> getCifInfo(@PathVariable String codId) {
        return ResponseEntity.ok(cifInfoService.getStructureInfo(codId));
    }
}
