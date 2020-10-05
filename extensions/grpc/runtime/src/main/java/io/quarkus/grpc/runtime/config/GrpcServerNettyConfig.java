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

}
