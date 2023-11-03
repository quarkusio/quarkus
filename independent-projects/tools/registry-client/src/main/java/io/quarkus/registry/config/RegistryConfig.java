package io.quarkus.registry.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.registry.json.JsonBuilder;

/**
 * Client side registry configuration containing information how to
 * communicate and resolve various catalogs from the registry.
 */
public interface RegistryConfig {

    /**
     * Registry ID. Mainly used in the logging and error messages to refer to a specific registry.
     *
     * @return registry id, never null
     */
    String getId();

    /**
     * Whether this registry should be included in the active registry list.
     * If the registry is disabled the client won't be sending any requests to it.
     *
     * @return true, if the registry is enabled, otherwise - false
     */
    boolean isEnabled();

    /**
     * How often (if ever) the locally cached catalogs provided by the registry
     * should be refreshed. The value returned by the method should currently be
     * <code>always</code>, <code>daily</code> (default), <code>interval:XXX</code>
     * (in minutes) or <code>never</code> (only if it doesn't exist locally).
     *
     * @return update policy
     */
    String getUpdatePolicy();

    /**
     * How to get the descriptor from the registry. A registry descriptor is the default
     * client configuration for the registry that can be customized on the client side, if necessary.
     *
     * @return registry descriptor related configuration
     */
    RegistryDescriptorConfig getDescriptor();

    /**
     * How get platform catalogs from the registry.
     *
     * @return platform catalog related configuration
     */
    RegistryPlatformsConfig getPlatforms();

    /**
     * How to get catalogs of non-platform extensions from the registry.
     *
     * @return non-platform extension catalog related configuration
     */
    RegistryNonPlatformExtensionsConfig getNonPlatformExtensions();

    /**
     * Registry client Maven related configuration, such as repository URL, etc.
     *
     * @return registry client Maven related configuration
     */
    RegistryMavenConfig getMaven();

    /**
     * Registry specific Quarkus version filtering configuration. For example,
     * a given registry may provide platform and extension information that are based
     * on specific versions of Quarkus core. Properly configured configured may
     * reduce the amount of unnecessary remote registry requests.
     *
     * @return Quarkus version filtering configuration
     */
    RegistryQuarkusVersionsConfig getQuarkusVersions();

    /**
     * Custom registry client configuration.
     *
     * @return custom registry client configuration
     */
    Map<String, Object> getExtra();

    /**
     * @return a mutable copy of this configuration
     */
    default Mutable mutable() {
        return new RegistryConfigImpl.Builder(this);
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

    interface Mutable extends RegistryConfig, JsonBuilder<RegistryConfig> {
        Mutable setId(String id);

        Mutable setEnabled(boolean enabled);

        Mutable setUpdatePolicy(String updatePolicy);

        Mutable setDescriptor(RegistryDescriptorConfig descriptor);

        Mutable setPlatforms(RegistryPlatformsConfig platforms);

        Mutable setNonPlatformExtensions(RegistryNonPlatformExtensionsConfig nonPlatformExtensionsConfig);

        Mutable setMaven(RegistryMavenConfig maven);

        Mutable setQuarkusVersions(RegistryQuarkusVersionsConfig quarkusVersions);

        default Mutable setAny(String name, Object value) {
            setExtra(name, value);
            return this;
        }

        Mutable setExtra(Map<String, Object> extra);

        Mutable setExtra(String name, Object value);

        /** @return an immutable copy of this configuration */
        RegistryConfig build();

        default void persist(Path p) throws IOException {
            RegistriesConfigMapperHelper.serialize(this.build(), p);
        }
    }

    /**
     * Get the default registry
     */
    static RegistryConfig defaultConfig() {
        return RegistryConfigImpl.getDefaultRegistry();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new RegistryConfigImpl.Builder();
    }

    /**
     * Read config from the specified file
     *
     * @param path File to read from (yaml or json)
     * @return read-only RegistryConfig object
     */
    static RegistryConfig fromFile(Path path) throws IOException {
        return RegistriesConfigMapperHelper.deserialize(path, RegistryConfigImpl.class);
    }

    /**
     * Read config from the specified file
     *
     * @param path File to read from (yaml or json)
     * @return mutable (possibly incomplete) RegistryConfig object
     */
    static Mutable mutableFromFile(Path path) throws IOException {
        return RegistriesConfigMapperHelper.deserialize(path, RegistryConfigImpl.Builder.class);
    }
}
