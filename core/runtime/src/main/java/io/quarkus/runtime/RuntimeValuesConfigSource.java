package io.quarkus.runtime;

import java.util.Set;

import io.smallrye.config.common.AbstractConfigSource;

/**
 * A <code>ConfigSource</code> to bridge between <code>Config</code> and {@link RuntimeValues}.
 * <p>
 * While {@link RuntimeValues} shouldn't be exposed as <code>Config</code>, this is intended to
 * work as a temporary compatibility layer, since until the introduction of {@link RuntimeValues},
 * the norm was to use <code>Config</code> to relay this kind of information.
 * <p>
 * This should be kept until we decide on an alternate solution in the discussion
 * <a href="https://github.com/quarkusio/quarkus/discussions/46915">#46915</a>.
 */
public class RuntimeValuesConfigSource extends AbstractConfigSource {
    public RuntimeValuesConfigSource() {
        super(RuntimeValuesConfigSource.class.getName(), Integer.MAX_VALUE - 10);
    }

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }

    @Override
    public String getValue(String propertyName) {
        return RuntimeValues.getValue(propertyName);
    }
}
