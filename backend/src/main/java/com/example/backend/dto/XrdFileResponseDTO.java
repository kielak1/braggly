// src/main/java/com/example/backend/dto/XrdFileResponseDTO.java
package com.example.backend.dto;

import com.example.backend.model.XrdFile;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XrdFileResponseDTO {
    private Long id;
    private String userFilename;
    private String originalFilename;
    private String storedFilename;
    private boolean publicVisible;
    private String sample;
    private String sampleDescription;
    private String site;
    private String institutionUser;
    private String dateMeasured;
    private LocalDateTime uploadedAt;

    public static XrdFileResponseDTO from(XrdFile file) {
        return XrdFileResponseDTO.builder()
                .id(file.getId())
                .userFilename(file.getUserFilename())
                .originalFilename(file.getOriginalFilename())
                .storedFilename(file.getStoredFilename())
                .publicVisible(file.isPublicVisible())
                .sample(file.getSample())
                .sampleDescription(file.getSampleDescription())
                .site(file.getSite())
                .institutionUser(file.getInstitutionUser())
                .dateMeasured(file.getDateMeasured())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
}