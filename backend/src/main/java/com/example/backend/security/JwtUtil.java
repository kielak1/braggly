package com.example.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;

@Component
public class JwtUtil {

    // 🔹 Poprawnie wygenerowany klucz o długości 256 bitów (32 bajty)
    // Klucz służy do podpisywania i weryfikacji tokenów JWT, zapewniając ich integralność i autentyczność.
    private final SecretKey SECRET_KEY;

    public JwtUtil(@Value("${jwt.secret}") String secretBase64) {
        this.SECRET_KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBase64));
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
           //     .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // Token ważny przez 1h
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // Token ważny przez   10min
                .signWith(SECRET_KEY)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean validateToken(String token, String username) {
        return (username.equals(extractUsername(token)) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
