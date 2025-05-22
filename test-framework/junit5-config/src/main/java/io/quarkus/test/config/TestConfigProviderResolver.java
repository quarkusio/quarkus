package io.quarkus.test.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.deployment.dev.testing.TestConfigCustomizer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * A {@link org.eclipse.microprofile.config.spi.ConfigProviderResolver} to register {@link Config} in the Test
 * classloader.
 */
public class TestConfigProviderResolver extends SmallRyeConfigProviderResolver {

    // Note that this class both *extends* and *consumes* SmallRyeConfigProviderResolver. Every method in SmallRyeConfigProviderResolver should be replicated here with a delegation to the instance variable, or there will be subtle and horrible bugs.
    private final SmallRyeConfigProviderResolver resolver;
    private final ClassLoader classLoader;
    private final Map<LaunchMode, SmallRyeConfig> configs;

    TestConfigProviderResolver() {
        this.resolver = (SmallRyeConfigProviderResolver) SmallRyeConfigProviderResolver.instance();
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.configs = new ConcurrentHashMap<>();
    }

    @Override
    public Config getConfig() {
        return resolver.getConfig();
    }

    /**
     * Registers a config in the Test classloader, by {@link LaunchMode}. Required for tests that launch Quarkus in
     * Dev mode (which uses the <code>dev</code> config profile, instead of <code>test</code>.
     * <p>
     * Retrieving the {@link Config} in a {@link LaunchMode} other than {@link LaunchMode#TEST}, must call
     * {@link TestConfigProviderResolver#restoreConfig()} after using the config, to avoid mismatches in the config
     * profile through the stack.
     *
     * @param mode the {@link LaunchMode}
     * @return the registed {@link Config} instance
     */
    public Config getConfig(final LaunchMode mode) {
        if (classLoader.equals(Thread.currentThread().getContextClassLoader())) {
            resolver.releaseConfig(classLoader);
            SmallRyeConfig config = configs.computeIfAbsent(mode, new Function<LaunchMode, SmallRyeConfig>() {
                @Override
                public SmallRyeConfig apply(final LaunchMode launchMode) {
                    LaunchMode current = LaunchMode.current();
                    LaunchMode.set(launchMode);
                    SmallRyeConfig config = ConfigUtils.configBuilder(false, true, mode)
                            .withCustomizers(new TestConfigCustomizer(mode))
                            .build();
                    LaunchMode.set(current);
                    return config;
                }
            });
            resolver.registerConfig(config, classLoader);
            return config;
        }
        throw new IllegalStateException("Context ClassLoader mismatch. Should be " + classLoader + " but was "
                + Thread.currentThread().getContextClassLoader());
    }

    public void restoreConfig() {
        if (classLoader.equals(Thread.currentThread().getContextClassLoader())) {
            resolver.releaseConfig(classLoader);
            resolver.registerConfig(configs.get(LaunchMode.TEST), classLoader);
        } else {
            throw new IllegalStateException("Context ClassLoader mismatch. Should be " + classLoader + " but was "
                    + Thread.currentThread().getContextClassLoader());
        }
    }

    public void restore() {
        this.configs.clear();
        ConfigProviderResolver.setInstance(resolver);
    }

    @Override
    public Config getConfig(final ClassLoader loader) {
        return resolver.getConfig(loader);
    }

    @Override
    public SmallRyeConfigBuilder getBuilder() {
        return resolver.getBuilder();
    }

    @Override
    public void registerConfig(final Config config, final ClassLoader classLoader) {
        resolver.registerConfig(config, classLoader);
    }

    @Override
    public void releaseConfig(final Config config) {
        resolver.releaseConfig(config);
    }

    @Override
    public void releaseConfig(final ClassLoader classLoader) {
        resolver.releaseConfig(classLoader);
    }
}
