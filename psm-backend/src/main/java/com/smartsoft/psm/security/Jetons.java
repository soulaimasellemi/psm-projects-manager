package com.smartsoft.psm.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Jetons à usage unique envoyés par email (validation de compte, mot de passe oublié).
 * Le jeton en clair part uniquement dans l'email ; la base ne stocke que son empreinte SHA-256.
 */
public final class Jetons {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Jetons() {}

    /** Jeton aléatoire de 256 bits, utilisable dans une URL */
    public static String generer() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
