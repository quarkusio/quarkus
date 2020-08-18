package io.quarkus.runtime.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfig;

public class ConfigProfileFileTestCase {

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
        final SmallRyeConfig config = ConfigUtils.configBuilder(true).build();
        cpr.registerConfig(config, classLoader);
        return config;
    }

    @Test
    void shouldLoadProdProfileFile() throws IOException {
        final SmallRyeConfig config = buildConfig();
        assertEquals("v2", config.getValue("foo.version", String.class));
    }

}