// src/main/java/com/example/backend/controller/XrdController.java
package com.example.backend.controller;

import com.example.backend.model.XrdData;
import com.example.backend.service.XrdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/xrd")
public class XrdController {

    @Autowired
    private XrdService xrdService;

    @PostMapping("/analyze")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") // Wymaga autoryzacji dla USER lub ADMIN
    public ResponseEntity<XrdData> analyzeFile(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        XrdData result = xrdService.analyzeXrdFile(file);
        return ResponseEntity.ok(result);
    }
}