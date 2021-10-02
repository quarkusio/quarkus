package io.quarkus.kafka.streams.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class KeyConfig {
    /**
     * Password of the private key in the key store
     */
    @ConfigItem
    public Optional<String> password;
}
