// src/main/java/com/example/backend/controller/XrdController.java
package com.example.backend.controller;

import com.example.backend.dto.XrdFileResponseDTO;
import com.example.backend.model.User;
import com.example.backend.model.XrdData;
import com.example.backend.model.XrdFile;
import com.example.backend.service.XrdFileService;
import com.example.backend.service.XrdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/xrd")
@Tag(name = "XRD Files", description = "Obsługa plików XRD i ich analizy")
public class XrdController {

    private final XrdService xrdService;
    private final XrdFileService xrdFileService;

    @Autowired
    public XrdController(XrdService xrdService, XrdFileService xrdFileService) {
        this.xrdService = xrdService;
        this.xrdFileService = xrdFileService;
    }

    @Operation(summary = "Analiza pliku XRD", description = "Przyjmuje plik UXD, wykonuje analizę i zwraca dane analizy.")
    @PostMapping("/analyze")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<XrdData> analyzeFile(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        XrdData result = xrdService.analyzeXrdFile(file);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Wgrywanie pliku UXD", description = "Zapisuje plik UXD i metadane w bazie danych i Backblaze B2.")
    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<XrdFileResponseDTO> uploadXrdFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String userFilename,
            @RequestParam(defaultValue = "true") boolean isPublic,
            @AuthenticationPrincipal User user) {
        try {
            XrdFile xrdFile = xrdFileService.saveUxdFile(file, userFilename, isPublic, user);
            return ResponseEntity.ok(XrdFileResponseDTO.from(xrdFile));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Lista plików użytkownika", description = "Zwraca listę plików UXD powiązanych z użytkownikiem.")
    @GetMapping("/files")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<XrdFileResponseDTO>> listXrdFiles(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(xrdFileService.getFilesForUser(user));
    }

    @Operation(summary = "Pojedynczy plik UXD", description = "Zwraca metadane pojedynczego pliku UXD po ID.")
    @GetMapping("/files/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<XrdFileResponseDTO> getFile(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(xrdFileService.getFileForUser(id, user));
    }

    @Operation(summary = "Aktualizacja metadanych pliku", description = "Aktualizuje metadane pliku UXD: nazwę użytkownika, status publiczny.")
    @PutMapping("/files/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<XrdFileResponseDTO> updateFile(
            @PathVariable Long id,
            @RequestBody XrdFileResponseDTO updateDto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(xrdFileService.updateFileMetadata(id, updateDto, user));
    }

    @Operation(summary = "Usuwanie pliku UXD", description = "Usuwa plik z bazy danych oraz z Backblaze B2.")
    @DeleteMapping("/files/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id, @AuthenticationPrincipal User user) {
        xrdFileService.deleteFile(id, user);
        return ResponseEntity.noContent().build();
    }
}
