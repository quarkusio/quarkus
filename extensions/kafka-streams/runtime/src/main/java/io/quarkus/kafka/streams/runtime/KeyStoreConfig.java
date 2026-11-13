package io.quarkus.kafka.streams.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.Secret;

@ConfigGroup
public interface KeyStoreConfig {
    /**
     * Key store type
     */
    Optional<String> type();

    /**
     * Key store location
     */
    Optional<String> location();

    /**
     * Key store password
     */
    Optional<Secret<String>> password();

    /**
     * Key store private key
     */
    Optional<Secret<String>> key();

    /**
     * Key store certificate chain
     */
    Optional<String> certificateChain();
}
