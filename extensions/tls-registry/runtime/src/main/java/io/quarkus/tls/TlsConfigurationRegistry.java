package io.quarkus.tls;

import java.util.Optional;

public interface TlsConfigurationRegistry {

    /**
     * Returns the named transport layer security configuration.
     *
     * @param name the name
     * @return the configuration, empty if not configured.
     */
    Optional<TlsConfiguration> get(String name);

    /**
     * Returns the default transport layer security configuration.
     *
     * @return the configuration, empty if not configured.
     */
    Optional<TlsConfiguration> getDefault();

    /**
     * Registers a TLS configuration into the registry.
     * Note that only subsequents calls to {@link #get(String)} will return the configuration.
     * <p>
     * The passed configuration is not validated, so it's up to the caller to ensure the configuration is correct.
     *
     * @param name the name of the configuration, cannot be {@code null}, cannot be {@code <default>}.
     * @param configuration the configuration cannot be {@code null}.
     */
    void register(String name, TlsConfiguration configuration);

}
