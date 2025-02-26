package io.quarkus.test.junit.classloading;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class QuarkusTestConfigProviderResolver extends SmallRyeConfigProviderResolver {
    private final SmallRyeConfigProviderResolver resolver;

    public QuarkusTestConfigProviderResolver(final ClassLoader classLoader) {
        this.resolver = (SmallRyeConfigProviderResolver) SmallRyeConfigProviderResolver.instance();

        SmallRyeConfig config = ConfigUtils.configBuilder(false, true, LaunchMode.TEST)
                .withProfile(LaunchMode.TEST.getDefaultProfile())
                .withMapping(TestConfig.class, "quarkus.test")
                .forClassLoader(classLoader)
                .build();

        this.registerConfig(config, Thread.currentThread().getContextClassLoader());
    }
}
