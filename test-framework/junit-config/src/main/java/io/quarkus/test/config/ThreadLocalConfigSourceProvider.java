package io.quarkus.test.config;

import io.smallrye.config.Config;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * A {@link java.lang.ThreadLocal} {@link org.eclipse.microprofile.config.spi.ConfigSourceProvider}, for Quarkus
 * dev mode and integration tests.
 * <p>
 * These kind of tests may require additional configuration coming from dev services, test resources or test
 * profiles. For such cases, the test creates a new {@link io.smallrye.config.Config} with the additional configuration
 * and replaces the current config with the new config on each test execution.
 */
public class ThreadLocalConfigSourceProvider extends SmallRyeConfigProviderResolver {
    private static final ThreadLocal<Config> CURRENT = new ThreadLocal<>();

    @Override
    public Config getConfig() {
        Config config = CURRENT.get();
        return config != null ? config : super.getConfig();
    }

    @Override
    public Config getConfig(ClassLoader classLoader) {
        // Do we need to support CL lookup?
        Config config = CURRENT.get();
        return config != null ? config : super.getConfig(classLoader);
    }

    @Override
    public SmallRyeConfig get() {
        Config config = CURRENT.get();
        return config != null ? config.unwrap(SmallRyeConfig.class) : super.get();
    }

    @Override
    public SmallRyeConfig get(ClassLoader classLoader) {
        // Do we need to support CL lookup?
        Config config = CURRENT.get();
        return config != null ? config.unwrap(SmallRyeConfig.class) : super.get(classLoader);
    }

    public static void set(final Config config) {
        CURRENT.set(config);
    }

    public static void reset() {
        CURRENT.remove();
    }
}
