package com.example.backend.model;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Schema(description = "Model przechowujący informacje o kredytach użytkownika")
public class UserCredits {

    @Id
    @Schema(description = "Unikalny identyfikator użytkownika, powiązany z modelem User", example = "101")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    @Schema(description = "Obiekt użytkownika powiązany z kredytami")
    private User user;

    @Schema(description = "Saldo dostępnych kredytów użytkownika", example = "500")
    private int balance;

    @Schema(description = "Data i czas ostatniej aktualizacji salda", example = "2024-03-13T14:30:00")
    private LocalDateTime lastUpdated;

    // Konstruktor bezargumentowy (wymagany przez JPA)
    public UserCredits() {}

    // Konstruktor z parametrami
    public UserCredits(User user, int balance) {
        this.user = user;
        this.userId = user.getId();
        this.balance = balance;
        this.lastUpdated = LocalDateTime.now();
    }
}
