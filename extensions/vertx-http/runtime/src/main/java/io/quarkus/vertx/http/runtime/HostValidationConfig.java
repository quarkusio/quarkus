package io.quarkus.vertx.http.runtime;

import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

/**
 * HTTP Host header validation.
 */
public interface HostValidationConfig {

    /**
     * Require that HTTP Host authority can only contain valid localhost names
     * such as "localhost", "127.0.0.1", "[::1]" or "::1".
     * <p>
     * This requirement is enforced in production and dev modes, when neither this nor the {@link #allowedHosts()} properties
     * are configured and the "quarkus.http.host" property has a valid localhost name.
     * Set this property to `false` if it not required.
     * <p>
     * Note this property is mutually exclusive with the {@link #allowedHosts()} property
     */
    Optional<Boolean> requireLocalhost();

    /**
     * Allowed hosts.
     * <p>
     * A comma-separated set of hosts, for example: "localhost", "quarkus.io".
     * <p>
     * Note this property is mutually exclusive with the {@link #requireLocalhost()} property.
     */
    Optional<Set<@WithConverter(TrimmedStringConverter.class) String>> allowedHosts();
}
