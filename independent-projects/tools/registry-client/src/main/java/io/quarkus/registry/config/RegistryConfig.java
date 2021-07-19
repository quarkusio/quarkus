package io.quarkus.registry.config;

import java.util.Map;

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
}
