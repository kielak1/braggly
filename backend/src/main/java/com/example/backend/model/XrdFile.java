package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XrdFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String userFilename;
    private String originalFilename;
    private String storedFilename;

    @Builder.Default
    @Column(name = "is_public")
    private boolean publicVisible = true;
    
    

    private String sample;
    private String sampleDescription;
    private String site;
    private String institutionUser;
    private String anode;
    private String detectorSlit;
    private String dateMeasured;
    private Double wavelength1;
    private Double wavelength2;
    private Double wavelength3;
    private Double stepTime;
    private Double stepSize;
    private Integer kv;

    private LocalDateTime uploadedAt;

    @PrePersist
    public void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
