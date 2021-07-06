package io.quarkus.runtime.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class UUIDPropertyTest {

    static ClassLoader classLoader;
    static ConfigProviderResolver cpr;

    @BeforeAll
    public static void initConfig() {
        classLoader = Thread.currentThread().getContextClassLoader();
        cpr = ConfigProviderResolver.instance();
    }

    @AfterEach
    public void doAfter() {
        try {
            cpr.releaseConfig(cpr.getConfig());
        } catch (IllegalStateException ignored) {
            // just means no config was installed, which is fine
        }
    }

    private SmallRyeConfig buildConfig() {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.withDefaultValue(ConfigUtils.UUID_KEY, UUID.randomUUID().toString());
        final SmallRyeConfig config = builder.build();
        cpr.registerConfig(config, classLoader);
        return config;
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
