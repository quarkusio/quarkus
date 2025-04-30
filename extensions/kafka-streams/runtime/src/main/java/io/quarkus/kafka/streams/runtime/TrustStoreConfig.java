package io.quarkus.kafka.streams.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface TrustStoreConfig {
    /**
     * Trust store type
     */
    Optional<String> type();

    /**
     * Trust store location
     */
    Optional<String> location();

    /**
     * Trust store password
     */
    Optional<String> password();

    /**
     * Trust store certificates
     */
    Optional<String> certificates();
}
