package io.quarkus.vertx.core.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ClusterConfiguration {

    /**
     * The host name.
     */
    @ConfigItem(defaultValue = "localhost")
    public String host;

    /**
     * The port.
     */
    @ConfigItem
    public OptionalInt port;

    /**
     * The public host name.
     */
    @ConfigItem
    public Optional<String> publicHost;

    /**
     * The public port.
     */
    @ConfigItem
    public OptionalInt publicPort;

    /**
     * Enables or disables the clustering.
     */
    @ConfigItem
    public boolean clustered;

    /**
     * The ping interval.
     */
    @ConfigItem(defaultValue = "20")
    public Duration pingInterval;

    /**
     * The ping reply interval.
     */
    @ConfigItem(defaultValue = "20")
    public Duration pingReplyInterval;
}
