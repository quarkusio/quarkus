package io.quarkus.test.junit.classloading;

import io.quarkus.deployment.dev.testing.TestConfigCustomizer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class QuarkusTestConfigProviderResolver extends SmallRyeConfigProviderResolver {
    private final SmallRyeConfigProviderResolver resolver;

    public QuarkusTestConfigProviderResolver() {
        this.resolver = (SmallRyeConfigProviderResolver) SmallRyeConfigProviderResolver.instance();

        ClassLoader classLoader = this.getClass().getClassLoader();
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            SmallRyeConfig config = ConfigUtils.configBuilder(false, true, LaunchMode.TEST)
                    .forClassLoader(classLoader)
                    .withCustomizers(new TestConfigCustomizer(LaunchMode.TEST))
                    .build();

            // See comments on AbstractJVMTestExtension#evaluateExecutionCondition for why this is the system classloader
            this.registerConfig(config, ClassLoader.getSystemClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
