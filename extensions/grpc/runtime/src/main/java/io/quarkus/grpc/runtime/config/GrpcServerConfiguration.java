package io.quarkus.grpc.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigGroup
public class GrpcServerConfiguration {

    /**
     * Do we use separate HTTP server to serve gRPC requests.
     * Set this to false if you want to use new Vert.x gRPC support,
     * which uses existing Vert.x HTTP server.
     */
    @ConfigItem(defaultValue = "true")
    public boolean useSeparateServer;

    /**
     * Configure XDS usage, if enabled.
     */
    @ConfigItem
    @ConfigDocSection(generated = true)
    public Xds xds;

    /**
     * Configure InProcess usage, if enabled.
     */
    @ConfigItem
    public InProcess inProcess;

    /**
     * The gRPC Server port.
     */
    @ConfigItem(defaultValue = "9000")
    public int port;

    /**
     * The gRPC Server port used for tests.
     */
    @ConfigItem(defaultValue = "9001")
    public int testPort;

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
     * <p>
     * When using a single server (using {@code quarkus.grpc.server.use-separate-server=false}), the default value is 256KB.
     * When using a separate server (using {@code quarkus.grpc.server.use-separate-server=true}), the default value is 4MB.
     */
    @ConfigItem
    public OptionalInt maxInboundMessageSize;

    /**
     * The max inbound metadata size in bytes
     */
    @ConfigItem
    public OptionalInt maxInboundMetadataSize;

    /**
     * The SSL/TLS config.
     */
    public SslServerConfig ssl;

    /**
     * Disables SSL, and uses plain text instead.
     * If disabled, configure the ssl configuration.
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

    /**
     * Number of gRPC server verticle instances.
     * This is useful for scaling easily across multiple cores.
     * The number should not exceed the amount of event loops.
     */
    @ConfigItem(defaultValue = "1")
    public int instances;

    /**
     * Configures the netty server settings.
     */
    @ConfigItem
    public GrpcServerNettyConfig netty;

    /**
     * gRPC compression, e.g. "gzip"
     */
    @ConfigItem
    public Optional<String> compression;
}
