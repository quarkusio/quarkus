package io.quarkus.runtime.configuration;

import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.SmallRyeConfigBuilder;

public class FixedAtBuildTimeConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        builder.setAddDefaultSources(false);
        builder.withSources(new JvmEnvironmentConfigSource());
        return builder;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    /**
     * A minimal config source that exposes only a small set of standard JVM system properties
     * needed for {@code @WithDefault} expression resolution. These describe the runtime
     * environment (temp directory, home directory, working directory) and are not configuration
     * overrides. Uses the lowest possible ordinal so frozen values always take precedence.
     */
    private static class JvmEnvironmentConfigSource implements ConfigSource {
        private static final Set<String> ALLOWED_PROPERTIES = Set.of(
                "java.io.tmpdir",
                "user.home",
                "user.dir");

        @Override
        public Set<String> getPropertyNames() {
            // Not listed to avoid being reported as unknown. Expression resolution only needs getValue().
            return Set.of();
        }

        @Override
        public String getValue(final String propertyName) {
            if (ALLOWED_PROPERTIES.contains(propertyName)) {
                return System.getProperty(propertyName);
            }
            return null;
        }

        @Override
        public String getName() {
            return "FixedAtBuildTimeJvmEnvironment";
        }

        @Override
        public int getOrdinal() {
            return Integer.MIN_VALUE;
        }
    }
}
