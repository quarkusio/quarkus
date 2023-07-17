package io.quarkus.grpc.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigGroup
public class GrpcClientConfiguration {

    public static final String DNS = "dns";
    public static final String XDS = "xds";

    /**
     * Use new Vert.x gRPC client support.
     * By default, we still use previous Java gRPC support.
     */
    @ConfigItem(defaultValue = "false")
    public boolean useQuarkusGrpcClient;

    /**
     * Configure XDS usage, if enabled.
     */
    @ConfigItem
    public ClientXds xds;

    /**
     * Configure InProcess usage, if enabled.
     */
    @ConfigItem
    public InProcess inProcess;

    /**
     * Configure Stork usage with new Vert.x gRPC, if enabled.
     */
    @ConfigItem
    public StorkConfig stork;

    /**
     * The gRPC service port.
     */
    @ConfigItem(defaultValue = "9000")
    public int port;

    /**
     * The host name / IP on which the service is exposed.
     */
    @ConfigItem(defaultValue = "localhost")
    public String host;

    /**
     * The SSL/TLS config.
     */
    public SslClientConfig ssl;

    /**
     * Use a name resolver. Defaults to dns.
     * If set to "stork", host will be treated as SmallRye Stork service name
     */
    @ConfigItem(defaultValue = DNS)
    public String nameResolver;

    /**
     * Whether {@code plain-text} should be used instead of {@code TLS}.
     * Enabled by default, except if TLS/SSL is configured. In this case, {@code plain-text} is disabled.
     */
    @ConfigItem
    public Optional<Boolean> plainText;

    /**
     * The duration after which a keep alive ping is sent.
     */
    @ConfigItem
    public Optional<Duration> keepAliveTime;

    /**
     * The flow control window in bytes. Default is 1MiB.
     */
    @ConfigItem
    public OptionalInt flowControlWindow;

    /**
     * The duration without ongoing RPCs before going to idle mode.
     */
    @ConfigItem
    public Optional<Duration> idleTimeout;

    /**
     * The amount of time the sender of a keep alive ping waits for an acknowledgement.
     */
    @ConfigItem
    public Optional<Duration> keepAliveTimeout;

    /**
     * Whether keep-alive will be performed when there are no outstanding RPC on a connection.
     */
    @ConfigItem(defaultValue = "false")
    public boolean keepAliveWithoutCalls;

    /**
     * The max number of hedged attempts.
     */
    @ConfigItem(defaultValue = "5")
    public int maxHedgedAttempts;

    /**
     * The max number of retry attempts.
     * Retry must be explicitly enabled.
     */
    @ConfigItem(defaultValue = "5")
    public int maxRetryAttempts;

    /**
     * The maximum number of channel trace events to keep in the tracer for each channel or sub-channel.
     */
    @ConfigItem
    public OptionalInt maxTraceEvents;

    /**
     * The maximum message size allowed for a single gRPC frame (in bytes).
     * Default is 4 MiB.
     */
    @ConfigItem
    public OptionalInt maxInboundMessageSize;

    /**
     * The maximum size of metadata allowed to be received (in bytes).
     * Default is 8192B.
     */
    @ConfigItem
    public OptionalInt maxInboundMetadataSize;

    /**
     * The negotiation type for the HTTP/2 connection.
     * Accepted values are: {@code TLS}, {@code PLAINTEXT_UPGRADE}, {@code PLAINTEXT}
     */
    @ConfigItem(defaultValue = "TLS")
    public String negotiationType;

    /**
     * Overrides the authority used with TLS and HTTP virtual hosting.
     */
    @ConfigItem
    public Optional<String> overrideAuthority;

    /**
     * The per RPC buffer limit in bytes used for retry.
     */
    @ConfigItem
    public OptionalLong perRpcBufferLimit;

    /**
     * Whether retry is enabled.
     * Note that retry is disabled by default.
     */
    @ConfigItem(defaultValue = "false")
    public boolean retry;

    /**
     * The retry buffer size in bytes.
     */
    @ConfigItem
    public OptionalLong retryBufferSize;

    /**
     * Use a custom user-agent.
     */
    @ConfigItem
    public Optional<String> userAgent;

    /**
     * Use a custom load balancing policy.
     * Accepted values are: {@code pick_first}, {@code round_robin}, {@code grpclb}.
     * This value is ignored if name-resolver is set to 'stork'.
     */
    @ConfigItem(defaultValue = "pick_first")
    public String loadBalancingPolicy;

    /**
     * The compression to use for each call. The accepted values are {@code gzip} and {@code identity}.
     */
    @ConfigItem
    public Optional<String> compression;

    /**
     * The deadline used for each call.
     * <p>
     * The format uses the standard {@link java.time.Duration} format. You can also provide duration values starting with a
     * number. In this case, if the value consists only of a number, the converter treats the value as seconds. Otherwise,
     * {@code PT} is implicitly prepended to the value to obtain a standard {@link java.time.Duration} format.
     */
    @ConfigItem
    public Optional<Duration> deadline;
}
