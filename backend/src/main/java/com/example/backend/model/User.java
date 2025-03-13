package com.example.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.persistence.*;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Schema(description = "Model użytkownika systemu, który przechowuje dane logowania oraz role użytkownika")
public class User implements UserDetails {

    public enum Role {
        ADMIN, USER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unikalny identyfikator użytkownika", example = "1")
    private Long id;

    @Column(unique = true, nullable = false)
    @Schema(description = "Unikalna nazwa użytkownika", example = "john_doe")
    private String username;

    @Column(nullable = false)
    @Schema(description = "Hasło użytkownika (przechowywane w formie zaszyfrowanej)", example = "$2a$10$...")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Rola użytkownika w systemie", example = "ADMIN")
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Schema(description = "Obiekt przechowujący saldo kredytowe użytkownika")
    private UserCredits userCredits;

    public User() {
    }

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.userCredits = new UserCredits(this, 0); // Automatyczne utworzenie powiązanego rekordu
    }

    @Schema(description = "Pobiera identyfikator użytkownika")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Schema(description = "Pobiera rolę użytkownika")
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Schema(description = "Pobiera saldo kredytowe użytkownika")
    public UserCredits getUserCredits() {
        return userCredits;
    }

    public void setUserCredits(UserCredits userCredits) {
        this.userCredits = userCredits;
        if (userCredits != null) {
            userCredits.setUser(this); // Synchronizacja referencji
        }
    }

    @Override
    @Schema(description = "Zwraca uprawnienia użytkownika", example = "[\"ROLE_ADMIN\"]")
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    @Schema(description = "Zwraca zaszyfrowane hasło użytkownika")
    public String getPassword() {
        return password;
    }

    @Override
    @Schema(description = "Zwraca nazwę użytkownika")
    public String getUsername() {
        return username;
    }

    @Override
    @Schema(description = "Czy konto użytkownika nie jest wygasłe?", example = "true")
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @Schema(description = "Czy konto użytkownika nie jest zablokowane?", example = "true")
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @Schema(description = "Czy poświadczenia użytkownika nie wygasły?", example = "true")
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @Schema(description = "Czy użytkownik jest aktywny?", example = "true")
    public boolean isEnabled() {
        return true;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
