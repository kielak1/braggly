package com.example.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Schema(description = "Model reprezentujący pakiet kredytowy dostępny w systemie")
public class CreditPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unikalny identyfikator pakietu kredytowego", example = "1")
    private Long id;

    @Schema(description = "Liczba kredytów w pakiecie", example = "100")
    private int credits;

    @Schema(description = "Cena pakietu w groszach (np. 5000 oznacza 50.00 PLN)", example = "5000")
    private int priceInCents;
}
