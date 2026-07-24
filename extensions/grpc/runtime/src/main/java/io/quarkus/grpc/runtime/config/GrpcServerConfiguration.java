package io.quarkus.grpc.runtime.config;

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

    /**
     * Whether to propagate the exception cause chain to gRPC clients using response trailers.
     * <p>
     * Disable for externally exposed services to avoid leaking internal error details.
     */
    @WithDefault("true")
    boolean propagateExceptionCauses();
}
