package io.quarkus.analytics.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.messagewriter.MessageWriter;

class ExtensionsFilterTest {

    private static final MessageWriter log = MessageWriter.info();

    @Test
    void discardTest() {
        assertFalse(ExtensionsFilter.onlyPublic("must.not.be.authorized", log));
        assertFalse(ExtensionsFilter.onlyPublic(null, log));
        assertFalse(ExtensionsFilter.onlyPublic("", log));
    }

    @Test
    void acceptTest() {
        assertTrue(ExtensionsFilter.onlyPublic("io.quarkus", log));
        assertTrue(ExtensionsFilter.onlyPublic("io.quarkus.something", log));
        assertTrue(ExtensionsFilter.onlyPublic("io.quarkiverse", log));
        assertTrue(ExtensionsFilter.onlyPublic("io.quarkiverse.something", log));
    }
}
