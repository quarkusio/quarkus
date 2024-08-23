package io.quarkus.grpc.runtime.config;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigGroup
public class GrpcServerNettyConfig {

    /**
     * Sets a custom keep-alive duration. This configures the time before sending a `keepalive` ping
     * when there is no read activity.
     */
    @ConfigItem
    public Optional<Duration> keepAliveTime;

    /**
     * Sets a custom permit-keep-alive duration. This configures the most aggressive keep-alive time clients
     * are permitted to configure.
     * The server will try to detect clients exceeding this rate and when detected will forcefully close the connection.
     *
     * @see #permitKeepAliveWithoutCalls
     */
    @ConfigItem
    public Optional<Duration> permitKeepAliveTime;

    /**
     * Sets whether to allow clients to send keep-alive HTTP/2 PINGs even if
     * there are no outstanding RPCs on the connection.
     */
    @ConfigItem
    public Optional<Boolean> permitKeepAliveWithoutCalls;

}
