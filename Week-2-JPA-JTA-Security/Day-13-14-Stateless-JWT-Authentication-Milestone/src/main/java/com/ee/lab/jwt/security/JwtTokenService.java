package com.ee.lab.jwt.security;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ISSUER = "jvm-pulse-security";
    private final byte[] secretKey;

    public JwtTokenService(String secretKeyString) {
        this.secretKey = secretKeyString.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generates a signed RFC-7519 JSON Web Token (JWT) with HMAC-SHA256.
     */
    public String generateToken(String username, Set<String> roles, long durationMillis) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(durationMillis);
        String jti = UUID.randomUUID().toString();

        // 1. JWT Header (typ: JWT, alg: HS256)
        JsonObject headerJson = Json.createObjectBuilder()
                .add("alg", "HS256")
                .add("typ", "JWT")
                .build();

        // 2. JWT Payload Claims (sub, iss, iat, exp, jti, roles)
        JsonArrayBuilder rolesArray = Json.createArrayBuilder();
        for (String role : roles) {
            rolesArray.add(role);
        }

        JsonObject payloadJson = Json.createObjectBuilder()
                .add("sub", username)
                .add("iss", ISSUER)
                .add("iat", now.getEpochSecond())
                .add("exp", expiry.getEpochSecond())
                .add("jti", jti)
                .add("roles", rolesArray)
                .build();

        String headerBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.toString().getBytes(StandardCharsets.UTF_8));
        String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.toString().getBytes(StandardCharsets.UTF_8));

        String signingInput = headerBase64 + "." + payloadBase64;
        String signatureBase64 = computeHmacSha256(signingInput);

        return signingInput + "." + signatureBase64;
    }

    /**
     * Generates a cryptographically secure random refresh token string.
     */
    public String generateRefreshTokenString() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Parses, cryptographically verifies, and checks expiration of a JWT token.
     */
    public JwtClaims validateAndParseToken(String tokenString) throws TokenExpiredException, InvalidSignatureException, MalformedTokenException {
        if (tokenString == null || tokenString.trim().isEmpty()) {
            throw new MalformedTokenException("Token string is empty or null");
        }

        String[] parts = tokenString.split("\\.");
        if (parts.length != 3) {
            throw new MalformedTokenException("JWT must contain exactly 3 dot-separated parts (Header, Payload, Signature)");
        }

        String headerBase64 = parts[0];
        String payloadBase64 = parts[1];
        String signatureBase64 = parts[2];

        // 1. Verify HMAC Signature
        String expectedSignature = computeHmacSha256(headerBase64 + "." + payloadBase64);
        if (!slowEquals(signatureBase64.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidSignatureException("JWT cryptographic signature verification failed! Token may have been tampered with.");
        }

        // 2. Decode and Parse Payload JSON
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadBase64);
            try (JsonReader jsonReader = Json.createReader(new StringReader(new String(payloadBytes, StandardCharsets.UTF_8)))) {
                JsonObject payload = jsonReader.readObject();

                String sub = payload.getString("sub");
                String iss = payload.getString("iss", ISSUER);
                long iatSec = payload.getJsonNumber("iat").longValue();
                long expSec = payload.getJsonNumber("exp").longValue();
                String jti = payload.getString("jti", UUID.randomUUID().toString());

                Set<String> roles = new HashSet<>();
                if (payload.containsKey("roles")) {
                    JsonArray rolesArray = payload.getJsonArray("roles");
                    for (int i = 0; i < rolesArray.size(); i++) {
                        roles.add(rolesArray.getString(i));
                    }
                }

                JwtClaims claims = new JwtClaims(
                        sub,
                        iss,
                        Instant.ofEpochSecond(iatSec),
                        Instant.ofEpochSecond(expSec),
                        roles,
                        jti
                );

                // 3. Check Expiration
                if (claims.isExpired()) {
                    throw new TokenExpiredException("JWT token has expired at " + claims.getExpiresAt());
                }

                return claims;
            }
        } catch (TokenExpiredException e) {
            throw e;
        } catch (Exception e) {
            throw new MalformedTokenException("Failed to decode and parse JWT claims payload", e);
        }
    }

    private String computeHmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 signature", e);
        }
    }

    private boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
