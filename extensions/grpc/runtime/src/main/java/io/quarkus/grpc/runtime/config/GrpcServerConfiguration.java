package io.quarkus.grpc.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface GrpcServerConfiguration {

    /**
     * The max inbound message size in bytes.
     */
    OptionalInt maxInboundMessageSize();

    /**
     * The maximum time a connection may exist before it is gracefully shut down (the gRPC
     * {@code MAX_CONNECTION_AGE} setting).
     * <p>
     * When a connection reaches this age, the server sends a {@code GOAWAY} frame so that the client
     * re-establishes a new connection. This is useful in Kubernetes environments where long-lived HTTP/2
     * connections prevent clients from discovering new server instances.
     * <p>
     * A jitter of ±10% is applied to avoid connection storms.
     * If not set, connections have an unlimited lifetime.
     */
    Optional<Duration> maxConnectionAge();

    /**
     * The grace period given to in-flight calls to complete after the connection reaches its maximum age
     * (the gRPC {@code MAX_CONNECTION_AGE_GRACE} setting).
     * <p>
     * Once the grace period expires, remaining calls are cancelled and the connection is closed.
     * If not set, in-flight calls are given an unlimited amount of time to complete.
     * <p>
     * This setting is only effective when {@code quarkus.grpc.server.max-connection-age} is set.
     */
    Optional<Duration> maxConnectionAgeGrace();

    /**
     * Enables the gRPC Reflection Service.
     * By default, the reflection service is only exposed in {@code dev} mode.
     * This setting allows overriding this choice and enable the reflection service every time.
     */
    @WithDefault("false")
    boolean enableReflectionService();

    /**
     * gRPC compression, e.g. "gzip"
     */
    Optional<String> compression();
}
