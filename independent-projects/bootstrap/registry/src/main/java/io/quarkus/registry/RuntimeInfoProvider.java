package io.quarkus.registry;

import io.quarkus.registry.ValueRegistry.RuntimeKey;

/**
 * A way to register {@link ValueRegistry.RuntimeInfo} with {@link ValueRegistry}. Implementations are free to
 * choose how {@link RuntimeInfoProvider} are discovered. The recommended approach is to use the ServiceLoader
 * mechanism.
 */
public interface RuntimeInfoProvider {
    /**
     * To register {@link ValueRegistry.RuntimeInfo} or {@link ValueRegistry.RuntimeKey} when {@link ValueRegistry}
     * is instantiated.
     *
     * @param valueRegistry the current {@link ValueRegistry}
     * @param runtimeSource a {@link RuntimeSource} with runtime values
     */
    void register(ValueRegistry valueRegistry, RuntimeSource runtimeSource);

    /**
     * A source that can query runtime values to populate {@link ValueRegistry}. Implementations are free to choose
     * which values are available on registration.
     *
     * @see RuntimeInfoProvider#register(ValueRegistry, RuntimeSource)
     */
    interface RuntimeSource {
        <T> T get(RuntimeKey<T> key);
    }
}
