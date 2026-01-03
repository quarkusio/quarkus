package io.quarkus.registry;

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
     */
    void register(ValueRegistry valueRegistry);
}
