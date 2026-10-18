package io.quarkus.kubernetes.deployment;

import static io.smallrye.config.PropertiesConfigSourceLoader.inClassPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class KubernetesConfigFallbackTest {
    @Test
    void fallback() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(
                        inClassPath("application-kubernetes.properties", 250, Thread.currentThread().getContextClassLoader()))
                .addDiscoveredCustomizers()
                .withConverter(Duration.class, 100, new DurationConverter())
                .withMappingIgnore("quarkus.**")
                .withMapping(KubernetesConfig.class)
                .withMapping(OpenShiftConfig.class)
                .withMapping(KnativeConfig.class)
                .build();

        KubernetesConfig kubernetes = config.getConfigMapping(KubernetesConfig.class);
        OpenShiftConfig openShift = config.getConfigMapping(OpenShiftConfig.class);
        KnativeConfig knative = config.getConfigMapping(KnativeConfig.class);

        assertTrue(kubernetes.name().isPresent());
        assertTrue(openShift.name().isPresent());
        assertEquals("naruto", kubernetes.name().get());
        assertEquals("sasuke", openShift.name().get());
        assertEquals(knative.name(), kubernetes.name());

        assertEquals(kubernetes.partOf(), openShift.partOf());

        for (Map.Entry<String, String> entry : kubernetes.labels().entrySet()) {
            assertTrue(openShift.labels().containsKey(entry.getKey()));
            assertEquals(openShift.labels().get(entry.getKey()), entry.getValue());
            assertTrue(knative.labels().containsKey(entry.getKey()));
            assertEquals(knative.labels().get(entry.getKey()), entry.getValue());
        }
    }

    @Test
    void sharedOnlyBetweenKubernetesAndOpenshift() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDiscoveredCustomizers()
                .withConverter(Duration.class, 100, new DurationConverter())
                .withMappingIgnore("quarkus.**")
                .withMapping(KubernetesConfig.class)
                .withMapping(OpenShiftConfig.class)
                .withMapping(KnativeConfig.class)
                .withSources(new PropertiesConfigSource(Map.of("quarkus.kubernetes.init-task-defaults.enabled", "false"), ""))
                .build();

        KubernetesConfig kubernetes = config.getConfigMapping(KubernetesConfig.class);
        OpenShiftConfig openShift = config.getConfigMapping(OpenShiftConfig.class);

        assertFalse(kubernetes.initTaskDefaults().enabled());
        assertFalse(openShift.initTaskDefaults().enabled());
    }
}
