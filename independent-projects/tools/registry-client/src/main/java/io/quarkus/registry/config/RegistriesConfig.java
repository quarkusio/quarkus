package io.quarkus.registry.config;

import java.util.List;

/**
 * Registry client configuration. Consists of a list of registry configurations that will be
 * providing platform and extension information to the client.
 */
public interface RegistriesConfig {

    /**
     * Enables or disables registry client debug mode.
     *
     * @return true if the debug mode should be enabled, otherwise - false
     */
    boolean isDebug();

    /**
     * A list of registries that should queried when generating catalogs of platforms and extensions.
     *
     * @return list of registries that should queried when generating catalogs of platforms and extensions
     */
    List<RegistryConfig> getRegistries();

    /**
     * @return a mutable copy of this registry configuration
     */
    MutableRegistriesConfig mutable();

    /**
     * @return ConfigSource that describes origin of this configuration
     */
    ConfigSource getSource();
}
