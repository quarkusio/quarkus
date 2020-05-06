package io.quarkus.grpc.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.grpc.runtime.GrpcTransportSecurity;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class GrpcServerConfiguration {

    /**
     * The gRPC Server port.
     */
    @ConfigItem(defaultValue = "9000")
    public int port;

    /**
     * The gRPC server host.
     */
    @ConfigItem(defaultValue = "0.0.0.0")
    public String host;

    /**
     * The gRPC handshake timeout.
     */
    @ConfigItem
    public Optional<Duration> handshakeTimeout;

    /**
     * The max inbound message size in bytes.
     */
    public @ConfigItem OptionalInt maxInboundMessageSize;

    /**
     * The SSL config.
     */
    public SslConfig ssl;

    /**
     * Disables SSL, and uses plain text instead.
     * If disables, configure the ssl configuration.
     */
    @ConfigItem(defaultValue = "true")
    public boolean plainText;

    /**
     * Whether ALPN should be used.
     */
    @ConfigItem(defaultValue = "true")
    public boolean alpn;

    /**
     * Configures the transport security.
     */
    @ConfigItem
    public GrpcTransportSecurity transportSecurity;

    /**
     * Enables the gRPC Reflection Service.
     * By default, the reflection service is only exposed in `dev` mode.
     * This setting allows overriding this choice and enable the reflection service every time.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableReflectionService;
}
