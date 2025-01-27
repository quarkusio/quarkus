package io.quarkus.runtime.configuration;

import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 * The initial configuration for Runtime.
 */
public class RuntimeConfigBuilder implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        new QuarkusConfigBuilderCustomizer().configBuilder(builder);
        builder.withSources(new UuidConfigSource());

        builder.forClassLoader(Thread.currentThread().getContextClassLoader())
                .addDefaultInterceptors()
                .addDefaultSources();
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    private static class UuidConfigSource implements ConfigSource {

        private static final String QUARKUS_UUID = "quarkus.uuid";

        @Override
        public Set<String> getPropertyNames() {
            // Not listed to avoid being reported as unknown. It can't be mapped either, because config will eagerly access it.
            return Set.of();
        }

        @Override
        public String getValue(String propertyName) {
            if (propertyName.equals(QUARKUS_UUID)) {
                return Holder.UUID_VALUE;
            }
            return null;
        }

        @Override
        public String getName() {
            return "QuarkusUUIDConfigSource";
        }

        @Override
        public int getOrdinal() {
            return Integer.MIN_VALUE;
        }

        // acts as a lazy value supplier ensuring that the UUID will only be produced when requested
        private static class Holder {
            private static final String UUID_VALUE = UUID.randomUUID().toString();
        }
    }
}
