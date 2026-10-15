package io.quarkus.kafka.streams.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.Secret;

@ConfigGroup
public interface KeyConfig {
    /**
     * Password of the private key in the key store
     */
    Optional<Secret<String>> password();
}
