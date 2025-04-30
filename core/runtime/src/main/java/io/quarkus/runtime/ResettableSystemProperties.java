package io.quarkus.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility that allows for setting system properties when it's created and resetting them when it's closed.
 * This is meant to be used in try-with-resources statements
 */
public class ResettableSystemProperties implements AutoCloseable {

    private final Map<String, String> toRestore;

    public ResettableSystemProperties(Map<String, String> toSet) {
        Objects.requireNonNull(toSet);
        if (toSet.isEmpty()) {
            toRestore = Collections.emptyMap();
            return;
        }
        toRestore = new HashMap<>();
        for (var entry : toSet.entrySet()) {
            String oldValue = System.setProperty(entry.getKey(), entry.getValue());
            toRestore.put(entry.getKey(), oldValue);
        }
    }

    public static ResettableSystemProperties of(String name, String value) {
        return new ResettableSystemProperties(Map.of(name, value));
    }

    public static ResettableSystemProperties empty() {
        return new ResettableSystemProperties(Collections.emptyMap());
    }

    @Override
    public void close() {
        for (var entry : toRestore.entrySet()) {
            if (entry.getValue() != null) {
                System.setProperty(entry.getKey(), entry.getValue());
            } else {
                System.clearProperty(entry.getKey());
            }
        }
    }
}
