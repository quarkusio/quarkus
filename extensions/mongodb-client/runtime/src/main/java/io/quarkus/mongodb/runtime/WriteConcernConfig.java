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
     * If set to {@code true}: the driver ensures that all writes are acknowledged by the MongoDB server, or else
     * throws an exception. (see also {@code w} and {@code wtimeoutMS}).
     * If set fo
     * <li>{@code false}: the driver does not ensure that all writes are acknowledged by the MongoDB server.
     */
    @ConfigItem(defaultValue = "true")
    public boolean safe;

    /**
     * Configures the journal writing aspect.
     * If set to {@code true}: the driver waits for the server to group commit to the journal file on disk.
     * If set to {@code false}: the driver does not wait for the server to group commit to the journal file on disk.
     */
    @ConfigItem(defaultValue = "true")
    public boolean journal;

    /**
     * When set, the driver adds {@code w: wValue} to all write commands. It requires {@code safe} to be {@code true}.
     * The value is typically a number, but can also be the {@code majority} string.
     */
    @ConfigItem
    public Optional<String> w;

    /**
     * If set to {@code true}, the driver will retry supported write operations if they fail due to a network error.
     */
    @ConfigItem
    public boolean retryWrites;

    /**
     * When set, the driver adds {@code wtimeout : ms } to all write commands. It requires {@code safe} to be
     * {@code true}.
     */
    @ConfigItem
    public Optional<Duration> wTimeout;

}
