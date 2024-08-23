package io.quarkus.vertx.core.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface ClusterConfiguration {

    /**
     * The host name.
     */
    @WithDefault("localhost")
    String host();

    /**
     * The port.
     */
    OptionalInt port();

    /**
     * The public host name.
     */
    Optional<String> publicHost();

    /**
     * The public port.
     */
    OptionalInt publicPort();

    /**
     * Enables or disables the clustering.
     */
    @WithDefault("false")
    boolean clustered();

    /**
     * The ping interval.
     */
    @WithDefault("20")
    Duration pingInterval();

    /**
     * The ping reply interval.
     */
    @WithDefault("20")
    Duration pingReplyInterval();
}
