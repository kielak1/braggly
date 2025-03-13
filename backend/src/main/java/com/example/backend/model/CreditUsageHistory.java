package com.example.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Schema(description = "Historia wykorzystania kredytów przez użytkowników")
public class CreditUsageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unikalny identyfikator rekordu użycia kredytów", example = "1")
    private Long id;

    @Schema(description = "Identyfikator użytkownika, który użył kredytów", example = "101")
    private Long userId;

    @Schema(description = "Typ wykorzystania kredytów (np. 'SUBSCRIPTION', 'PAYMENT')", example = "SUBSCRIPTION")
    private String usageType;

    @Schema(description = "Data i czas wykorzystania kredytów", example = "2024-03-13T14:30:00")
    private LocalDateTime usageDate;

    @Schema(description = "Liczba użytych kredytów", example = "50")
    private int creditsUsed;
}
