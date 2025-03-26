package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.model.XrdData;
import com.example.backend.model.XrdFile;
import com.example.backend.service.XrdService;
import com.example.backend.service.XrdFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.backend.dto.XrdFileResponseDTO;

import java.io.IOException;

@RestController
@RequestMapping("/api/xrd")
public class XrdController {

    private final XrdService xrdService;
    private final XrdFileService xrdFileService;

    @Autowired
    public XrdController(XrdService xrdService, XrdFileService xrdFileService) {
        this.xrdService = xrdService;
        this.xrdFileService = xrdFileService;
    }

    @PostMapping("/analyze")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<XrdData> analyzeFile(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        XrdData result = xrdService.analyzeXrdFile(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadXrdFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String userFilename,
            @RequestParam(defaultValue = "true") boolean publicVisible,
            @AuthenticationPrincipal User user) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Nie przesłano pliku.");
        }

        try {
            XrdFile xrdFile = xrdFileService.saveUxdFile(file, userFilename, publicVisible, user);

            XrdFileResponseDTO dto = XrdFileResponseDTO.builder()
                    .id(xrdFile.getId())
                    .userFilename(xrdFile.getUserFilename())
                    .originalFilename(xrdFile.getOriginalFilename())
                    .storedFilename(xrdFile.getStoredFilename())
                    .publicVisible(xrdFile.isPublicVisible())
                    .sample(xrdFile.getSample())
                    .sampleDescription(xrdFile.getSampleDescription())
                    .site(xrdFile.getSite())
                    .institutionUser(xrdFile.getInstitutionUser())
                    .dateMeasured(xrdFile.getDateMeasured())
                    .uploadedAt(xrdFile.getUploadedAt())
                    .build();

            return ResponseEntity.ok(dto);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Błąd podczas przetwarzania pliku: " + e.getMessage());
        }
    }
}
