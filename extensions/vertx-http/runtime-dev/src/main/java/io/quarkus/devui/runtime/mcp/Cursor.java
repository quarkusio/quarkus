package io.quarkus.devui.runtime.mcp;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

/**
 * A composite opaque cursor.
 */
record Cursor(Instant createdAt, String name) {

    static final Cursor FIRST_PAGE = new Cursor(Instant.EPOCH, null);

    static String encode(Instant createdAt, String name) {
        String value = createdAt + "$$$" + name;
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static Cursor decode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Blank cursor value");
        }
        String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\$\\$\\$");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid parts: " + Arrays.toString(parts));
        }
        Instant createdAt = Instant.parse(parts[0]);
        String name = parts[1];
        return new Cursor(createdAt, name);
    }

}
