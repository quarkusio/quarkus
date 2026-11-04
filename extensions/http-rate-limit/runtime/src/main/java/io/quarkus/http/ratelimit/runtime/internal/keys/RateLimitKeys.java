package io.quarkus.http.ratelimit.runtime.internal.keys;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RateLimitKeys {

    /**
     * Longer keys are replaced by their SHA-256, so that {@code max-keys} bounds the memory a store holds even when a
     * client controls the key, for example through a header.
     */
    public static final int MAX_KEY_LENGTH = 256;

    private static final String SHA256_PREFIX = "sha256:";

    private RateLimitKeys() {
    }

    /**
     * Also hashes a key that starts with {@code sha256:}, so that no key that is kept as is can equal a hashed one, and
     * a key that contains a brace, which would make part of it a Redis Cluster hash tag and pin every key sharing that
     * part to one shard.
     *
     * @return the key itself when it has at most {@value #MAX_KEY_LENGTH} characters, does not start with
     *         {@code sha256:} and contains no brace, otherwise {@code sha256:} followed by the lowercase hex SHA-256 of
     *         its UTF-8 bytes
     */
    public static String cap(String key) {
        if (key.length() <= MAX_KEY_LENGTH && !key.startsWith(SHA256_PREFIX) && key.indexOf('{') < 0
                && key.indexOf('}') < 0) {
            return key;
        }
        return sha256(key);
    }

    /**
     * @return {@code sha256:} followed by the lowercase hex SHA-256 of the UTF-8 bytes of {@code value}
     */
    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return SHA256_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // every Java platform implements SHA-256
            throw new IllegalStateException(e);
        }
    }
}
