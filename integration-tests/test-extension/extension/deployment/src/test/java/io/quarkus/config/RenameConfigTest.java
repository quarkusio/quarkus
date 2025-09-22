package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.config.rename.RenameConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class RenameConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    RenameConfig renameConfig;
    @Inject
    SmallRyeConfig config;

    @Test
    void rename() {
        assertEquals("1234", renameConfig.prop());
        assertEquals("1234", config.getConfigValue("quarkus.rename.prop").getValue());

        assertEquals("only-in-new", renameConfig.onlyInNew());
        assertEquals("only-in-old", renameConfig.onlyInOld());
        assertEquals("new", renameConfig.inBoth());

        // This will always return values. It lookups on "rename" first and "rename-old" next
        assertEquals("only-in-new", config.getConfigValue("quarkus.rename.only-in-new").getValue());
        assertEquals("only-in-old", config.getConfigValue("quarkus.rename.only-in-old").getValue());
        assertEquals("new", config.getConfigValue("quarkus.rename.in-both").getValue());

        assertEquals("only-in-new", config.getConfigValue("quarkus.rename-old.only-in-new").getValue());
        assertEquals("only-in-old", config.getConfigValue("quarkus.rename-old.only-in-old").getValue());
        assertEquals("new", config.getConfigValue("quarkus.rename-old.in-both").getValue());

        assertEquals("old-default", config.getConfigValue("quarkus.rename.with-default").getValue());
        assertEquals("old-default", config.getConfigValue("quarkus.rename-old.with-default").getValue());
        assertEquals("old-default", renameConfig.withDefault());

        // Make sure we only record the actual properties in the sources (and not renamed properties)
        Optional<ConfigSource> configSource = config.getConfigSource("BuildTime RunTime Fixed");
        assertTrue(configSource.isPresent());
        ConfigSource buildTimeRunTimeDefaults = configSource.get();

        // In Build time source
        assertNotNull(buildTimeRunTimeDefaults.getValue("quarkus.rename.prop"));
        assertNotNull(buildTimeRunTimeDefaults.getValue("quarkus.rename.only-in-new"));
        assertNotNull(buildTimeRunTimeDefaults.getValue("quarkus.rename.only-in-old"));
        assertNotNull(buildTimeRunTimeDefaults.getValue("quarkus.rename.in-both"));
        // When in both only the one that has priority (remamed) is recorded
        assertNull(buildTimeRunTimeDefaults.getValue("quarkus.rename-old.in-both"));
        // Relocate / Fallback properties, not in the source but values are not null when Config is queried
        assertNull(buildTimeRunTimeDefaults.getValue("quarkus.rename-old.prop"));
        assertNotNull(config.getConfigValue("quarkus.rename-old.prop").getValue());
        assertNull(buildTimeRunTimeDefaults.getValue("quarkus.rename-old.only-in-new"));
        assertNotNull(config.getConfigValue("quarkus.rename-old.only-in-new").getValue());
        assertNull(buildTimeRunTimeDefaults.getValue("quarkus.rename-old.only-in-old"));
        assertNotNull(config.getConfigValue("quarkus.rename-old.only-in-old").getValue());
    }
}
