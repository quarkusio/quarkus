package io.quarkus.runtime.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class UUIDPropertyTest {

    private SmallRyeConfig buildConfig() {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.withDefaultValue(ConfigUtils.UUID_KEY, UUID.randomUUID().toString());
        return builder.build();
    }

    @Test
    void testConfigSource() {
        SmallRyeConfig config = buildConfig();
        assertThat(config.getConfigSources()).isNotEmpty();
        ConfigValue value = config.getConfigValue(ConfigUtils.UUID_KEY);
        assertThat(value).isNotNull();
        assertThat(value.getValue()).isNotNull().isNotBlank();
    }

    @Test
    void testBuildTimeWithDevMode() {
        SmallRyeConfig config = ConfigUtils.configBuilder(false, LaunchMode.DEVELOPMENT).build();
        assertThat(config.getConfigSources()).isNotEmpty();
        ConfigValue value = config.getConfigValue(ConfigUtils.UUID_KEY);
        assertThat(value).isNotNull();
        assertThat(value.getValue()).isNull();
    }
}
