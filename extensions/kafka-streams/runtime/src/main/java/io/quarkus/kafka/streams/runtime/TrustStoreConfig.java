package io.quarkus.kafka.streams.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TrustStoreConfig {
    /**
     * Trust store type
     */
    @ConfigItem
    public Optional<String> type;

    /**
     * Trust store location
     */
    @ConfigItem
    public Optional<String> location;

    /**
     * Trust store password
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Trust store certificates
     */
    @ConfigItem
    public Optional<String> certificates;
}
