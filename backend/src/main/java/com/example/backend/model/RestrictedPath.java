package com.example.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Schema(description = "Model reprezentujący płatne ścieżki w systemie")
@Table(name = "restricted_paths")
public class RestrictedPath {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unikalny identyfikator ścieżki")
    private Long id;

    @Column(nullable = false, unique = true)
    @Schema(description = "Ścieżka wymagająca dodatniego salda", example = "/user/uploads")
    private String path;
}
// @Schema(description = "Model reprezentujący płatne ścieżki w systemie")
// @Table(name = "restricted_paths")

