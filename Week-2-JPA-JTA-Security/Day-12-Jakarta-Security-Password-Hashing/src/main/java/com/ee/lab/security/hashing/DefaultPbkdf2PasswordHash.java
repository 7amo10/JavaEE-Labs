package com.ee.lab.security.hashing;

import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Map;

public class DefaultPbkdf2PasswordHash implements Pbkdf2PasswordHash {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 2048;
    private static final int SALT_BYTE_LENGTH = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private int iterations = DEFAULT_ITERATIONS;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void initialize(Map<String, String> parameters) {
        if (parameters != null && parameters.containsKey("Pbkdf2PasswordHash.Iterations")) {
            this.iterations = Integer.parseInt(parameters.get("Pbkdf2PasswordHash.Iterations"));
        }
    }

    @Override
    public String generate(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        byte[] salt = new byte[SALT_BYTE_LENGTH];
        secureRandom.nextBytes(salt);

        byte[] hash = pbkdf2(password, salt, iterations, KEY_LENGTH_BITS);

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        // Format: PBKDF2:iterations:salt_base64:hash_base64
        return "PBKDF2:" + iterations + ":" + saltBase64 + ":" + hashBase64;
    }

    @Override
    public boolean verify(char[] password, String hashedPassword) {
        if (password == null || hashedPassword == null) {
            return false;
        }

        String[] parts = hashedPassword.split(":");
        if (parts.length != 4 || !parts[0].equals("PBKDF2")) {
            return false;
        }

        try {
            int parsedIterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            byte[] actualHash = pbkdf2(password, salt, parsedIterations, expectedHash.length * 8);

            return slowEquals(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to execute PBKDF2 hashing", e);
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
