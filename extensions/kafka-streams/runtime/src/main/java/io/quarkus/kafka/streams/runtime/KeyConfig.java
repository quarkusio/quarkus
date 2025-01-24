package io.quarkus.kafka.streams.runtime;

import java.util.Optional;

public interface KeyConfig {
    /**
     * Password of the private key in the key store
     */
    Optional<String> password();
}
