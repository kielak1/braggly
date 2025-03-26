// src/main/java/com/example/backend/dto/XrdFileResponseDTO.java
package com.example.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
}
