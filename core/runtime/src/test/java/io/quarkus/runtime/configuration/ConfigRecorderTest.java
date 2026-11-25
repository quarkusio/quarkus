package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConfigUtils.emptyConfigBuilder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.ConfigConfig;
import io.smallrye.config.Config;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;

class ConfigRecorderTest {
    private Config config;

    @AfterEach
    void releaseConfig() {
        ConfigProviderResolver.instance().releaseConfig(config);
    }

    @Test
    void buildTimeExpressionToSecretNotChanged() {
        registerConfig(Map.of(
                "build.fixed", "${secret}",
                "secret", "the-secret"));

        assertDoesNotThrow(() -> new ConfigRecorder().handleConfigChange(List.of(recorded("${secret}", "the-secret"))));
    }

    @Test
    void buildTimeExpressionToSecretChanged() {
        registerConfig(Map.of(
                "build.fixed", "${other.secret}",
                "secret", "the-secret",
                "other.secret", "the-other-secret"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new ConfigRecorder().handleConfigChange(List.of(recorded("${secret}", "the-secret"))));
        assertTrue(exception.getMessage()
                .contains("build.fixed is set to '${other.secret}' but it is build time fixed to '${secret}'"));
        assertFalse(exception.getMessage().contains("the-secret"));
        assertFalse(exception.getMessage().contains("the-other-secret"));
    }

    private void registerConfig(Map<String, String> properties) {
        config = emptyConfigBuilder()
                .withSources(new PropertiesConfigSource(properties, "test", 100))
                .withMapping(ConfigConfig.class)
                .withDefaultValue("quarkus.config.build-time-mismatch-at-runtime", "fail")
                .withSecretKeys("secret", "other.secret")
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    private static ConfigValue recorded(String rawValue, String value) {
        return ConfigValue.builder()
                .withName("build.fixed")
                .withValue(value)
                .withRawValue(rawValue)
                .withConfigSourceOrdinal(100)
                .build();
    }
}
