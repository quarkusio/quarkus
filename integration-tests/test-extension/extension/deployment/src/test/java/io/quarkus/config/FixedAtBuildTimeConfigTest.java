package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.config.SmallRyeConfig;

public class FixedAtBuildTimeConfigTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.config.fixed-at-build-time", "true")
            .overrideRuntimeConfigKey("quarkus.mapping.rt.value", "runtime-override");

    @Inject
    SmallRyeConfig config;

    @Test
    void frozenValueIsServedFromBuildTimeSource() {
        assertEquals("value", config.getRawValue("quarkus.mapping.rt.value"));
        assertEquals("BuildTime RunTime Fixed", config.getConfigValue("quarkus.mapping.rt.value").getConfigSourceName());
    }

    @Test
    void runtimeApplicationPropertiesChangeDoesNotOverride() {
        // overrideRuntimeConfigKey injects "runtime-override" as a runtime config override,
        // simulating an external source trying to change the value after the build
        assertEquals("value", config.getRawValue("quarkus.mapping.rt.value"));
    }

    @Test
    void systemPropertyDoesNotOverride() {
        System.setProperty("quarkus.mapping.rt.value", "sys-override");
        try {
            assertEquals("value", config.getRawValue("quarkus.mapping.rt.value"));
        } finally {
            System.clearProperty("quarkus.mapping.rt.value");
        }
    }

    @Test
    void externalConfigSourcesAreNotRegistered() {
        for (var source : config.getConfigSources()) {
            var name = source.getName();
            if (name.contains("EnvConfigSource") || name.contains("SysPropConfigSource")
                    || name.contains("PropertiesConfigSource")) {
                throw new AssertionError("External config source should not be registered: " + name);
            }
        }
    }

    @Test
    void unknownPropertyFromEnvironmentIsNotVisible() {
        // With external sources removed, a stray system property should not be visible to the config
        // system at all — so it cannot trigger "unrecognized property" warnings
        System.setProperty("quarkus.some.unknown.property", "stray-value");
        try {
            assertNull(config.getRawValue("quarkus.some.unknown.property"));
        } finally {
            System.clearProperty("quarkus.some.unknown.property");
        }
    }

    @Test
    void expressionWithJvmPropertyResolvesAtRuntime() {
        String original = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", "/custom/tmp");
        try {
            assertEquals("/custom/tmp/cache", config.getRawValue("fixed-at-build-time.cache-dir"));
        } finally {
            System.setProperty("java.io.tmpdir", original);
        }
    }

    @Test
    void withDefaultStillWorks() {
        var value = config.getRawValue("quarkus.mapping.rt.record-default");
        assertTrue(value != null && !value.isEmpty());
    }
}
