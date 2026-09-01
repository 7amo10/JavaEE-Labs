package com.ee.lab.jwt.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sec_refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    @Column(name = "token_string", nullable = false, unique = true, length = 128)
    private String tokenString;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RefreshToken() {
        this.createdAt = Instant.now();
        this.revoked = false;
    }

    public RefreshToken(String tokenString, String username, Instant expiresAt) {
        this.tokenString = tokenString;
        this.username = username;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.revoked = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTokenString() { return tokenString; }
    public void setTokenString(String tokenString) { this.tokenString = tokenString; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
