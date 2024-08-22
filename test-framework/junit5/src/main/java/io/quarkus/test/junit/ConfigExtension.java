package io.quarkus.test.junit;

import org.junit.jupiter.api.extension.Extension;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class ConfigExtension implements Extension {
    public static ClassLoader TEST_CONFIG = new TestConfigClassLoader();

    public ConfigExtension() {
        LaunchMode.set(LaunchMode.TEST);

        SmallRyeConfig config = ConfigUtils.configBuilder(true, true, LaunchMode.NORMAL)
                .withMapping(TestConfig.class)
                .build();
        SmallRyeConfigProviderResolver configProviderResolver = (SmallRyeConfigProviderResolver) SmallRyeConfigProviderResolver
                .instance();

        try {
            configProviderResolver.registerConfig(config, TEST_CONFIG);
        } catch (IllegalStateException e) {
            // Is there any scenario where the config is already registered?
        }
    }

    private static class TestConfigClassLoader extends ClassLoader {

    }
}
