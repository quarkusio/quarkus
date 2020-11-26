package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultConfigSource.PROPERTY_PREFIX;
import static io.quarkus.vault.runtime.config.VaultConfigSource.SECRET_CONFIG_KV_PREFIX_PATHS;
import static io.quarkus.vault.runtime.config.VaultConfigSource.SECRET_CONFIG_KV_PREFIX_PATH_PATTERN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;

public class VaultConfigSourceTest {

    private static final String PROPERTY = PROPERTY_PREFIX + SECRET_CONFIG_KV_PREFIX_PATHS;

    @Test
    void secretConfigKvPathPattern() {

        Matcher matcher;

        matcher = SECRET_CONFIG_KV_PREFIX_PATH_PATTERN.matcher(PROPERTY + ".hello");
        assertTrue(matcher.matches());
        assertEquals("hello", matcher.group(1));

        matcher = SECRET_CONFIG_KV_PREFIX_PATH_PATTERN.matcher(PROPERTY + ".\"mp.jwt.verify\"");
        assertTrue(matcher.matches());
        assertEquals("mp.jwt.verify", matcher.group(2));
    }
}
