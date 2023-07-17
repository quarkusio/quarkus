package io.quarkus.kafka.streams.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class KeyStoreConfig {
    /**
     * Key store type
     */
    @ConfigItem
    public Optional<String> type;

    /**
     * Key store location
     */
    @ConfigItem
    public Optional<String> location;

    /**
     * Key store password
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Key store private key
     */
    @ConfigItem
    public Optional<String> key;

    /**
     * Key store certificate chain
     */
    @ConfigItem
    public Optional<String> certificateChain;
}
