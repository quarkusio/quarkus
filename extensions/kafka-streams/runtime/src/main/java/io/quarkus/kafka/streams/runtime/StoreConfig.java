package io.quarkus.kafka.streams.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class StoreConfig {
    /**
     * Store type
     */
    @ConfigItem
    public Optional<String> type;

    /**
     * Store location
     */
    @ConfigItem
    public Optional<String> location;

    /**
     * Store password
     */
    @ConfigItem
    public Optional<String> password;
}
