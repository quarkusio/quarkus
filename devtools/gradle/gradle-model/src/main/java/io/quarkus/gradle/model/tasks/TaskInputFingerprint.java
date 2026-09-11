package io.quarkus.gradle.model.tasks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Produces deterministic opaque fingerprints for structured scalar Gradle task inputs.
 * <p>
 * Map entries are ordered by key and each UTF-8 key/value is length-prefixed before SHA-256 hashing, avoiding ambiguity
 * between adjacent fields. The result is intended for change detection, not as a security or external compatibility
 * contract.
 */
public final class TaskInputFingerprint {

    private TaskInputFingerprint() {
    }

    /**
     * Hashes all entries in key order.
     *
     * @param values non-null string keys and values
     * @return lowercase hexadecimal SHA-256 fingerprint
     */
    public static String ofMap(Map<String, String> values) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        new TreeMap<>(values).forEach((key, value) -> {
            update(digest, key);
            update(digest, value);
        });
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
