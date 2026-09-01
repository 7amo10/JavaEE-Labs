package com.ee.lab.jwt.security;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JwtClaims {

    private final String subject;
    private final String issuer;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final Set<String> roles;
    private final String jwtId;

    public JwtClaims(String subject, String issuer, Instant issuedAt, Instant expiresAt, Set<String> roles, String jwtId) {
        this.subject = subject;
        this.issuer = issuer;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.roles = Collections.unmodifiableSet(new HashSet<>(roles));
        this.jwtId = jwtId;
    }

    public String getSubject() { return subject; }
    public String getIssuer() { return issuer; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Set<String> getRoles() { return roles; }
    public String getJwtId() { return jwtId; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
