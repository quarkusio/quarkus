package io.quarkus.mongodb.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Configures the write concern.
 */
@ConfigGroup
public interface WriteConcernConfig {

    /**
     * Configures the safety.
     * If set to {@code true}: the driver ensures that all writes are acknowledged by the MongoDB server, or else
     * throws an exception. (see also {@code w} and {@code wtimeoutMS}).
     * If set fo
     * <li>{@code false}: the driver does not ensure that all writes are acknowledged by the MongoDB server.
     */
    @WithDefault("true")
    boolean safe();

    /**
     * Configures the journal writing aspect.
     * If set to {@code true}: the driver waits for the server to group commit to the journal file on disk.
     * If set to {@code false}: the driver does not wait for the server to group commit to the journal file on disk.
     */
    @WithDefault("true")
    boolean journal();

    /**
     * When set, the driver adds {@code w: wValue} to all write commands. It requires {@code safe} to be {@code true}.
     * The value is typically a number, but can also be the {@code majority} string.
     */
    Optional<String> w();

    /**
     * If set to {@code true}, the driver will retry supported write operations if they fail due to a network error.
     */
    @WithDefault("false")
    boolean retryWrites();

    /**
     * When set, the driver adds {@code wtimeout : ms } to all write commands. It requires {@code safe} to be
     * {@code true}.
     */
    Optional<Duration> wTimeout();

}
