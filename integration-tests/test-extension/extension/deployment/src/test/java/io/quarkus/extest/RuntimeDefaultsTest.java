package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.config.EnvBuildTimeConfigSource;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class RuntimeDefaultsTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsServiceProvider(ConfigSource.class, EnvBuildTimeConfigSource.class)
                    .addAsResource("application.properties"));

    @Inject
    SmallRyeConfig config;

    @Test
    void doNotRecordEnvRuntimeDefaults() {
        Optional<ConfigSource> defaultValues = config.getConfigSource("DefaultValuesConfigSource");
        assertTrue(defaultValues.isPresent());
        // Do not record Env values for runtime
        assertNull(defaultValues.get().getValue("quarkus.mapping.rt.do-not-record"));
        assertEquals("value", config.getConfigValue("quarkus.mapping.rt.do-not-record").getValue());
        // Property available in both Env and application.properties, ok to record application.properties value
        assertEquals("from-app", defaultValues.get().getValue("bt.ok.to.record"));
        // You still get the value from Env
        assertEquals("from-env", config.getConfigValue("bt.ok.to.record").getValue());
        // Do not record any of the other properties
        assertNull(defaultValues.get().getValue(("do.not.record")));
        assertNull(defaultValues.get().getValue(("DO_NOT_RECORD")));
        assertEquals("value", config.getConfigValue("do.not.record").getValue());
    }

    @Test
    void doNotRecordActiveUnprofiledPropertiesDefaults() {
        Optional<ConfigSource> defaultValues = config.getConfigSource("DefaultValuesConfigSource");
        assertTrue(defaultValues.isPresent());
        assertEquals("properties", config.getConfigValue("bt.profile.record").getValue());
        // Property needs to be recorded as is, including the profile name
        assertEquals("properties", defaultValues.get().getValue("%test.bt.profile.record"));
        assertNull(defaultValues.get().getValue("bt.profile.record"));
    }

    @Test
    void recordDefaultFromRootEvenIfInActiveProfile() {
        Optional<ConfigSource> defaultValues = config.getConfigSource("DefaultValuesConfigSource");
        assertTrue(defaultValues.isPresent());
        assertEquals("from-app", defaultValues.get().getValue("%test.quarkus.mapping.rt.record-default"));
        assertEquals("from-default", defaultValues.get().getValue("quarkus.mapping.rt.record-default"));
    }

    @Test
    void recordProfile() {
        Optional<ConfigSource> defaultValues = config.getConfigSource("DefaultValuesConfigSource");
        assertTrue(defaultValues.isPresent());
        assertEquals("record", config.getConfigValue("quarkus.profile").getValue());
    }
}
