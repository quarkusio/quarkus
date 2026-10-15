package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConfigUtils.emptyConfigBuilder;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;

class ConfigRecorderTest {
    private SmallRyeConfig config;

    @AfterEach
    void releaseConfig() {
        ConfigProviderResolver.instance().releaseConfig(config);
    }

    @Test
    void buildTimeExpressionToSecretNotChanged() {
        registerConfig(Map.of(
                "build.fixed", "${secret}",
                "secret", "the-secret"));

        assertThatCode(() -> new ConfigRecorder().handleConfigChange(List.of(recorded("${secret}", "the-secret"))))
                .doesNotThrowAnyException();
    }

    @Test
    void buildTimeExpressionToSecretChanged() {
        registerConfig(Map.of(
                "build.fixed", "${other.secret}",
                "secret", "the-secret",
                "other.secret", "the-other-secret"));

        assertThatThrownBy(() -> new ConfigRecorder().handleConfigChange(List.of(recorded("${secret}", "the-secret"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("build.fixed is set to '${other.secret}' but it is build time fixed to '${secret}'")
                .hasMessageNotContaining("the-secret")
                .hasMessageNotContaining("the-other-secret");
    }

    private void registerConfig(Map<String, String> properties) {
        config = emptyConfigBuilder()
                .withSources(new PropertiesConfigSource(properties, "test", 100))
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
