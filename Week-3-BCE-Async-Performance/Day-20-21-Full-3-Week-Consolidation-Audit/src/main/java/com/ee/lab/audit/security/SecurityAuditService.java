package com.ee.lab.audit.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityAuditService {

    private static final int SALT_LENGTH = 16;

    public String generateSaltedHash(String plainPassword) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(plainPassword.getBytes());

            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);
            return saltBase64 + ":" + hashBase64;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    public boolean verifyPassword(String plainPassword, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) return false;

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] actualHash = md.digest(plainPassword.getBytes());

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    public String createStatelessToken(String username, String role) {
        String payload = username + "|" + role + "|" + (System.currentTimeMillis() + 3600000);
        return Base64.getEncoder().encodeToString(payload.getBytes());
    }

    public boolean validateToken(String token, String requiredRole) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            String[] parts = decoded.split("\\|");
            if (parts.length != 3) return false;

            String role = parts[1];
            long expiry = Long.parseLong(parts[2]);

            if (System.currentTimeMillis() > expiry) return false;
            return requiredRole == null || requiredRole.equalsIgnoreCase(role);
        } catch (Exception e) {
            return false;
        }
    }
}
