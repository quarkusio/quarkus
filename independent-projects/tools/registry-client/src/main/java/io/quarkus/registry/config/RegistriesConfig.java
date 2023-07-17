package io.quarkus.registry.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.registry.json.JsonBuilder;

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
     * @return ConfigSource that describes origin of this configuration
     */
    ConfigSource getSource();

    /**
     * @return a mutable copy of this registry configuration
     */
    default Mutable mutable() {
        return new RegistriesConfigImpl.Builder(this);
    }

    /**
     * Persist this configuration to the specified file.
     *
     * @param p Target path
     * @throws IOException if the specified file can not be written to.
     */
    default void persist(Path p) throws IOException {
        RegistriesConfigMapperHelper.serialize(this, p);
    }

    /**
     * Persist this configuration to the original source (if possible).
     * Does nothing for configurations read from environment variables.
     *
     * @throws IOException if the source file can not be written to.
     */
    void persist() throws IOException;

    interface Mutable extends RegistriesConfig, JsonBuilder<RegistriesConfig> {
        /**
         * @param debug to enable debugging for this configuration
         * @return this. Use for chaining.
         */
        Mutable setDebug(boolean debug);

        /**
         * @param registries List of constructed Registry configurations
         * @return this. Use for chaining.
         */
        Mutable setRegistries(List<RegistryConfig> registries);

        /**
         * @param registryId The id to add (a RegistryConfig object will be created for it)
         * @return this. Use for chaining.
         */
        Mutable setRegistry(String registryId);

        /**
         * @param config A RegistryConfig object to add to the configuration
         * @return this. Use for chaining.
         */
        Mutable setRegistry(RegistryConfig config);

        /**
         * Add a registry to the configuration
         *
         * @param registryId The id to add (a RegistryConfig object will be created for it)
         * @return true if the registry was added
         */
        boolean addRegistry(String registryId);

        /**
         * Add a registry to the configuration
         *
         * @param config A RegistryConfig object to add to the configuration
         * @return true if the registry was added
         */
        boolean addRegistry(RegistryConfig config);

        /**
         * Remove a registry from the configuration
         *
         * @param registryId The id of the registry to remove
         * @return true if the registry was removed
         */
        boolean removeRegistry(String registryId);

        /** @return an immutable copy of this configuration */
        RegistriesConfig build();

        default void persist(Path p) throws IOException {
            RegistriesConfigMapperHelper.serialize(this.build(), p);
        }
    }

    /**
     * Resolve the active registries configuration from
     * system properties, the environment, and defaults.
     *
     * @return immutable RegistriesConfig
     */
    static RegistriesConfig resolveConfig() {
        return RegistriesConfigLocator.resolveConfig();
    }

    /**
     * Resolve the active registries configuration from
     * the specified file
     *
     * @param configYaml Yaml file to read from
     * @return read-only RegistriesConfig object
     */
    static RegistriesConfig resolveFromFile(Path configYaml) {
        return RegistriesConfigLocator.load(configYaml);
    }

    /**
     * Read config from the specified file
     *
     * @param path File to read from (yaml or json)
     * @return read-only RegistriesConfig object (will return defaults for an empty file)
     */
    static RegistriesConfig fromFile(Path path) throws IOException {
        return mutableFromFile(path).build();
    }

    /**
     * Read config from the specified file
     *
     * @param path File to read from (yaml or json)
     * @return mutable (possibly incomplete) RegistriesConfig object (will return defaults for an empty file)
     */
    static RegistriesConfig.Mutable mutableFromFile(Path path) throws IOException {
        RegistriesConfigImpl.Builder builder = RegistriesConfigMapperHelper.deserialize(path,
                RegistriesConfigImpl.Builder.class);
        return builder == null ? RegistriesConfig.builder() : builder;
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new RegistriesConfigImpl.Builder();
    }
}
