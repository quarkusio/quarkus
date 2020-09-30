package io.quarkus.grpc.runtime.config;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigGroup
public class GrpcServerNettyConfig {

    /**
     * Sets a custom keepalive time, the delay time for sending next keepalive ping.
     */
    @ConfigItem
    public Optional<Duration> keepAliveTime;

}
