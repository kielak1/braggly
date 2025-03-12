// package com.example.backend.model;

// import java.time.LocalDateTime;

// import jakarta.persistence.*;
// import lombok.Getter;
// import lombok.Setter;

// @Getter
// @Setter
// @Entity
// public class UserCredits {
//     @Id
//     private Long userId;
//     private int balance;
//     private LocalDateTime lastUpdated;
// }


package com.example.backend.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserCredits {
    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private int balance;
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
