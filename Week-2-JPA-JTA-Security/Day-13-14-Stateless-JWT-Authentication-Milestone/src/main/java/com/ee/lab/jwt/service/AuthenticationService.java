package com.ee.lab.jwt.service;

import com.ee.lab.jwt.dto.AuthResponse;
import com.ee.lab.jwt.dto.LoginRequest;
import com.ee.lab.jwt.entity.RefreshToken;
import com.ee.lab.jwt.entity.UserAccount;
import com.ee.lab.jwt.security.JwtTokenService;
import com.ee.lab.jwt.security.Pbkdf2PasswordService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

public class AuthenticationService {

    private static final long ACCESS_TOKEN_VALIDITY_MS = 15 * 60 * 1000; // 15 minutes
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7; // 7 days

    private final EntityManagerFactory emf;
    private final Pbkdf2PasswordService passwordService;
    private final JwtTokenService jwtTokenService;
    private final SecurityAuditService auditService;

    public AuthenticationService(EntityManagerFactory emf, Pbkdf2PasswordService passwordService,
                                 JwtTokenService jwtTokenService, SecurityAuditService auditService) {
        this.emf = emf;
        this.passwordService = passwordService;
        this.jwtTokenService = jwtTokenService;
        this.auditService = auditService;
    }

    public void seedUser(String username, String rawPassword, String email, Set<String> roles) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            String hash = passwordService.hashPassword(rawPassword);
            UserAccount user = new UserAccount(username, hash, email, roles);
            em.persist(user);
            em.getTransaction().commit();
            auditService.recordAudit("USER_SEED", username, "127.0.0.1", "SUCCESS", "User created with roles: " + roles);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public AuthResponse authenticate(LoginRequest request, String ipAddress) {
        EntityManager em = emf.createEntityManager();
        try {
            UserAccount user;
            try {
                user = em.createQuery("SELECT u FROM UserAccount u WHERE u.username = :username", UserAccount.class)
                        .setParameter("username", request.getUsername())
                        .getSingleResult();
            } catch (NoResultException e) {
                auditService.recordAudit("LOGIN_FAILURE", request.getUsername(), ipAddress, "FAILED", "User not found");
                throw new SecurityException("Invalid username or password");
            }

            if (!user.isEnabled()) {
                auditService.recordAudit("LOGIN_FAILURE", request.getUsername(), ipAddress, "LOCKED", "User account is disabled");
                throw new SecurityException("Account is locked");
            }

            boolean passwordMatch = passwordService.verifyPassword(request.getPassword(), user.getPasswordHash());
            if (!passwordMatch) {
                auditService.recordAudit("LOGIN_FAILURE", request.getUsername(), ipAddress, "FAILED", "Invalid password attempt");
                throw new SecurityException("Invalid username or password");
            }

            // 1. Generate Access JWT
            String accessToken = jwtTokenService.generateToken(user.getUsername(), user.getRoles(), ACCESS_TOKEN_VALIDITY_MS);

            // 2. Generate and Persist Refresh Token
            String refreshTokenString = jwtTokenService.generateRefreshTokenString();
            Instant refreshExpiry = Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);

            em.getTransaction().begin();
            RefreshToken refreshTokenEntity = new RefreshToken(refreshTokenString, user.getUsername(), refreshExpiry);
            em.persist(refreshTokenEntity);
            em.getTransaction().commit();

            auditService.recordAudit("LOGIN_SUCCESS", user.getUsername(), ipAddress, "SUCCESS", "Issued JWT and RefreshToken");

            return new AuthResponse(
                    accessToken,
                    ACCESS_TOKEN_VALIDITY_MS / 1000,
                    refreshTokenString,
                    user.getUsername(),
                    user.getRoles()
            );

        } finally {
            em.close();
        }
    }

    public AuthResponse refreshAccessToken(String refreshTokenString, String ipAddress) {
        EntityManager em = emf.createEntityManager();
        try {
            RefreshToken refreshToken;
            try {
                refreshToken = em.createQuery("SELECT r FROM RefreshToken r WHERE r.tokenString = :token", RefreshToken.class)
                        .setParameter("token", refreshTokenString)
                        .getSingleResult();
            } catch (NoResultException e) {
                auditService.recordAudit("REFRESH_FAILURE", "UNKNOWN", ipAddress, "REJECTED", "Refresh token not found");
                throw new SecurityException("Invalid refresh token");
            }

            if (refreshToken.isRevoked() || refreshToken.isExpired()) {
                auditService.recordAudit("REFRESH_FAILURE", refreshToken.getUsername(), ipAddress, "EXPIRED", "Token revoked or expired");
                throw new SecurityException("Refresh token is expired or revoked");
            }

            UserAccount user = em.createQuery("SELECT u FROM UserAccount u WHERE u.username = :username", UserAccount.class)
                    .setParameter("username", refreshToken.getUsername())
                    .getSingleResult();

            String newAccessToken = jwtTokenService.generateToken(user.getUsername(), user.getRoles(), ACCESS_TOKEN_VALIDITY_MS);
            auditService.recordAudit("REFRESH_SUCCESS", user.getUsername(), ipAddress, "SUCCESS", "Generated fresh JWT via refresh token");

            return new AuthResponse(
                    newAccessToken,
                    ACCESS_TOKEN_VALIDITY_MS / 1000,
                    refreshTokenString,
                    user.getUsername(),
                    user.getRoles()
            );
        } finally {
            em.close();
        }
    }
}
