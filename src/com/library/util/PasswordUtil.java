package com.library.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Passwords are never stored as plain text. We store a salted PBKDF2 hash
 * in the form "iterations:salt:hash" (salt and hash are Base64).
 */
public final class PasswordUtil {
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return ITERATIONS + ":" + b64.encodeToString(salt) + ":" + b64.encodeToString(hash);
    }

    public static boolean verify(String password, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            return MessageDigest.isEqual(expected, pbkdf2(password, salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        }
    }
}
