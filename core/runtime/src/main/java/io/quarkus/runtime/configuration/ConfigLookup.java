package io.quarkus.runtime.configuration;

import java.lang.invoke.MethodHandles;

import org.eclipse.microprofile.config.ConfigProvider;

import io.smallrye.config.SmallRyeConfig;

/**
 * Bootstrap methods for resolving configuration mappings as dynamic constants.
 * <p>
 * This class provides the {@code condy} (constant dynamic) bootstrap method used by
 * the ActionBuilder lambda transliteration system. When a lambda captures a
 * {@code @ConfigRoot(phase = BUILD_AND_RUN_TIME_FIXED)} configuration mapping,
 * the transliterator replaces the capture with a {@code CONSTANT_Dynamic} entry
 * whose bootstrap method is {@link #configMapping(MethodHandles.Lookup, String, Class)}.
 * The JVM lazily resolves the constant on first access and caches it thereafter.
 */
public final class ConfigLookup {

    private ConfigLookup() {
    }

    /**
     * Condy bootstrap method that resolves a {@code @ConfigMapping} interface
     * from the current SmallRye configuration.
     *
     * @param lookup the lookup context (unused, required by condy signature)
     * @param name the constant name (unused, required by condy signature)
     * @param type the configuration mapping interface class to resolve
     * @return the resolved configuration mapping instance
     */
    public static Object configMapping(MethodHandles.Lookup lookup, String name, Class<?> type) {
        return getConfigMapping(type);
    }

    /**
     * Resolve a {@code @ConfigMapping} interface from the current SmallRye configuration.
     * <p>
     * This method is called directly by generated {@code StartupTask} implementations
     * that provide runtime configuration mappings as services.
     *
     * @param type the configuration mapping interface class to resolve
     * @return the resolved configuration mapping instance
     */
    public static Object getConfigMapping(Class<?> type) {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getConfigMapping(type);
    }
}
