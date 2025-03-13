package com.example.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Schema(description = "Historia zakupów pakietów kredytowych przez użytkowników")
public class CreditPurchaseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unikalny identyfikator rekordu zakupu", example = "1")
    private Long id;

    @Schema(description = "Identyfikator użytkownika, który dokonał zakupu", example = "101")
    private Long userId;

    @Schema(description = "Liczba kredytów zakupionych przez użytkownika", example = "500")
    private int creditsPurchased;

    @Schema(description = "Kwota zapłacona w centach (np. 10000 oznacza 100,00 PLN)", example = "10000")
    private int amountPaid;

    @Schema(description = "Data i czas dokonania zakupu", example = "2024-03-13T14:30:00")
    private LocalDateTime purchaseDate;
}
