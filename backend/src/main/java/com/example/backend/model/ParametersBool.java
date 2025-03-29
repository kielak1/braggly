

package com.example.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Schema(description = "Model reprezentujący Parametry Bool")
@Table(name = "parameters_bool")
public class ParametersBool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unikalny identyfikator parametru")
    private Long id;

    @Column(unique = true, nullable = false)
    @Schema(description = "Nazwa parametru", example = "free_access")
    private String name;

    @Column(nullable = false)
    @Schema(description = "Wartość parametru", example = "true")
    private Boolean value;
}