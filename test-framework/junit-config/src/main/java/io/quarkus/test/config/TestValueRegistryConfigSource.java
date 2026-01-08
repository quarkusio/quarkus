package io.quarkus.test.config;

import java.util.Set;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

import io.quarkus.registry.ValueRegistry;
import io.quarkus.registry.ValueRegistry.RuntimeInfo;
import io.quarkus.runtime.ValueRegistryConfigSource;
import io.smallrye.config.common.AbstractConfigSource;

/**
 * A {@link org.eclipse.microprofile.config.spi.ConfigSource} to bridge between the Config System and
 * {@link ValueRegistry}. This allows the use of {@link io.smallrye.config.SmallRyeConfig} to look up values from
 * {@link ValueRegistry} in Integration tests.
 * <p>
 * While {@link ValueRegistry} shouldn't be exposed in the Config System, this is intended to work as a temporary
 * compatibility layer, since until the introduction of {@link ValueRegistry}, the norm was to use
 * {@link io.smallrye.config.SmallRyeConfig} and System Properties to relay this kind of information, which will be
 * moved to {@link ValueRegistry}, so we need this not to break code that is still relying on the Config system.
 * <p>
 * This should be kept until we decide on an alternate solution in the discussion
 * <a href="https://github.com/quarkusio/quarkus/discussions/46915">#46915</a>.
 */
public class TestValueRegistryConfigSource extends AbstractConfigSource {
    public static final ExtensionContext.Namespace CONFIG = ExtensionContext.Namespace.create(new Object());
    private static final Namespace LAUNCHER_CONFIG = Namespace.create(CONFIG.getParts());

    private final NamespacedHierarchicalStore<Namespace> store;

    TestValueRegistryConfigSource(NamespacedHierarchicalStore<Namespace> store) {
        // ordinal just a bit lower than Build Time Runtime fixed source and ValueRegistryConfigSource
        super(ValueRegistryConfigSource.class.getSimpleName(), Integer.MAX_VALUE - 20);
        this.store = store;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }

    @Override
    public String getValue(String propertyName) {
        ValueRegistry valueRegistry = store.get(LAUNCHER_CONFIG, ValueRegistry.class, ValueRegistry.class);
        if (valueRegistry != null) {
            RuntimeInfo<?> value = valueRegistry.get(propertyName);
            // TODO - We may be required to convert this to the expected config string in Config
            return value != null ? value.get(valueRegistry).toString() : null;
        }
        return null;
    }
}
