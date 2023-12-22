package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class RuntimeDefaultsTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    // Don't change this to types, because of classloader class cast exception.
                    .addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                            "io.quarkus.extest.runtime.config.EnvBuildTimeConfigSource")
                    .addAsResource("application.properties"));

    @Inject
    SmallRyeConfig config;

    @Test
    void doNotRecordEnvRuntimeDefaults() {
        Optional<ConfigSource> defaultValues = config.getConfigSource("DefaultValuesConfigSource");
        assertTrue(defaultValues.isPresent());
        // It's ok to record env properties for a Quarkus root
        assertEquals("changed", defaultValues.get().getValue("quarkus.rt.rt-string-opt"));
        // It's ok to record env properties for a property available in another source
        assertEquals("env-source", defaultValues.get().getValue("bt.ok.to.record"));
        // Do not record any of the other properties
        assertNull(defaultValues.get().getValue(("do.not.record")));
        assertNull(defaultValues.get().getValue(("DO_NOT_RECORD")));
    }

    @Test
    void doNotRecordActiveUnprofiledPropertiesDefaults() {
        Optional<ConfigSource> defaultValues = config.getConfigSource("DefaultValuesConfigSource");
        assertTrue(defaultValues.isPresent());
        assertEquals("properties", config.getRawValue("bt.profile.record"));
        assertEquals("properties", defaultValues.get().getValue("%test.bt.profile.record"));
        assertNull(defaultValues.get().getValue("bt.profile.record"));
    }
}
