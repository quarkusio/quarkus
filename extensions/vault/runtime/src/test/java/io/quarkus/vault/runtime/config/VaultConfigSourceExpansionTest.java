package io.quarkus.vault.runtime.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VaultConfigSourceExpansionTest {

    private Map<String, String> values = new HashMap<>();

    private VaultConfigSource source = new VaultConfigSource(0) {
        @Override
        protected String getBaseProperty(String propertyName, String defaultValue) {
            return values.getOrDefault(propertyName, defaultValue);
        }
    };

    @BeforeEach
    public void init() {
        values.put("my-vault-host", "localhost");
        values.put("port", "${offset}00");
        values.put("offset", "82");
        values.put("quarkus.vault.url", "http://${my-vault-host}:${port}");
        values.put("one", "${two}");
        values.put("two", "${three}");
        values.put("three", "${four}");
        values.put("four", "${five}");
        values.put("five", "5");
    }

    @Test
    void expansionPattern() {

        assertEquals("http://localhost:8200", source.getProperty("quarkus.vault.url", null, 0));

        Exception exception = assertThrows(RuntimeException.class, () -> source.getProperty("one", null, 0));
        assertEquals("max expansion depth reached when looking for key four", exception.getMessage());

        assertEquals("5", source.getProperty("one", null, 4));
    }
}
