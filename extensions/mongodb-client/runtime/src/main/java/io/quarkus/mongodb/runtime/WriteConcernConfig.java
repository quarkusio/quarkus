package io.quarkus.mongodb.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configures the write concern.
 */
@ConfigGroup
public class WriteConcernConfig {

    /**
     * Configures the safety.
     * If set to `true`: the driver ensures that all writes are acknowledged by the MongoDB server, or else
     * throws an exception. (see also `w` and `w-timeout`).
     * If set to `false`: the driver does not ensure that all writes are acknowledged by the MongoDB server.
     */
    @ConfigItem(defaultValue = "true")
    public boolean safe;

    /**
     * Configures the journal writing aspect.
     * If set to `true`: the driver waits for the server to group commit to the journal file on disk.
     * If set to `false`: the driver does not wait for the server to group commit to the journal file on disk.
     */
    @ConfigItem(defaultValue = "true")
    public boolean journal;

    /**
     * When set, the driver adds `w`: `wValue` to all write commands. It requires `safe` to be `true`.
     * The value is typically a number, but can also be the `majority` string.
     */
    @ConfigItem
    public Optional<String> w;

    /**
     * If set to `true`, the driver will retry supported write operations if they fail due to a network error.
     */
    @ConfigItem(defaultValue = "false")
    public boolean retryWrites;

    /**
     * When set, the driver adds `wtimeout : ms ` to all write commands. It requires `safe` to be
     * `true`.
     */
    @ConfigItem
    public Optional<Duration> wTimeout;

}
