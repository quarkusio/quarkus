package io.quarkus.extest.runtime;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.common.MapBackedConfigSource;

public class AdditionalStaticInitConfigSourceProvider implements ConfigSourceProvider {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        return Collections.singletonList(new AdditionalStaticInitConfigSource());
    }

    public static class AdditionalStaticInitConfigSource extends MapBackedConfigSource {
        public static AtomicInteger counter = new AtomicInteger(0);

        public AdditionalStaticInitConfigSource() {
            super("AdditionalStaticInitConfigSourceDeprecated", Collections.emptyMap());
            counter.incrementAndGet();
        }
    }
}
