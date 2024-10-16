package io.quarkus.vertx.core.runtime.config;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface PemKeyCertConfiguration {

    /**
     * PEM Key/cert config is disabled by default.
     */
    @WithParentName
    @WithDefault("false")
    boolean enabled();

    /**
     * Comma-separated list of the path to the key files (Pem format).
     */
    Optional<List<String>> keys();

    /**
     * Comma-separated list of the path to the certificate files (Pem format).
     */
    Optional<List<String>> certs();

}
