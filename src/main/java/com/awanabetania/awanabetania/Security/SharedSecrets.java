package com.awanabetania.awanabetania.Security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Comparison of configured secrets (admin PIN, NFC token) with values sent by a client. */
public final class SharedSecrets {

    private SharedSecrets() {
    }

    /**
     * {@code true} when {@code candidate} equals {@code expected}. Takes the same time
     * however many leading characters match, so response times reveal nothing about the
     * secret. An unset (blank) secret matches nothing, so a missing configuration value
     * locks the feature instead of opening it.
     */
    public static boolean matches(String expected, String candidate) {
        if (expected == null || expected.isBlank() || candidate == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                                     candidate.getBytes(StandardCharsets.UTF_8));
    }
}
