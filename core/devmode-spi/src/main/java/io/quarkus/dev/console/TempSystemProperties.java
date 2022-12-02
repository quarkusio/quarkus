package io.quarkus.dev.console;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: this is a hack, we should be able to pass config overrides into the bootstrap
 */
public class TempSystemProperties implements AutoCloseable {
    final Map<String, String> old = new HashMap<>();

    public void set(String key, String value) {
        old.put(key, System.getProperty(key));
        System.setProperty(key, value);
    }

    @Override
    public void close() {
        for (Map.Entry<String, String> e : old.entrySet()) {
            if (e.getValue() == null) {
                System.clearProperty(e.getKey());
            } else {
                System.setProperty(e.getKey(), e.getValue());
            }
        }
    }
}
