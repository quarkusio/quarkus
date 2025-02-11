package io.quarkus.grpc.runtime.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface GrpcClientConfiguration {

    String DNS = "dns";
    String XDS = "xds";

    /**
     * Use new Vert.x gRPC client support.
     * By default, we still use previous Java gRPC support.
     */
    @WithDefault("false")
    boolean useQuarkusGrpcClient();

    /**
     * Use Vert.x event loop(s) for gRPC client, if it's using the previous Java gRPC support.
     */
    @WithDefault("true")
    boolean useVertxEventLoop();

    /**
     * Configure XDS usage, if enabled.
     */
    @ConfigDocSection(generated = true)
    ClientXds xds();

    /**
     * Configure InProcess usage, if enabled.
     */
    InProcess inProcess();

    /**
     * Configure Stork usage with new Vert.x gRPC, if enabled.
     */
    StorkConfig stork();

    /**
     * The gRPC service port.
     */
    @WithDefault("9000")
    int port();

    /**
     * The gRPC service test port.
     */
    OptionalInt testPort();

    /**
     * The host name / IP on which the service is exposed.
     */
    @WithDefault("localhost")
    String host();

    /**
     * The SSL/TLS config.
     * Only use this if you want to use the old Java gRPC client.
     */
    SslClientConfig ssl();

    /**
     * The name of the TLS configuration to use.
     * <p>
     * If not set and the default TLS configuration is configured ({@code quarkus.tls.*}) then that will be used.
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     * <p>
     * If no TLS configuration is set, and {@code quarkus.tls.*} is not configured, then,
     * `quarkus.grpc.clients.$client-name.tls` will be used.
     * <p>
     * Important: This is only supported when using the Quarkus (Vert.x-based) gRPC client.
     */
    Optional<String> tlsConfigurationName();

    /**
     * The TLS config.
     * Only use this if you want to use the Quarkus gRPC client.
     */
    TlsClientConfig tls();

    /**
     * Use a name resolver. Defaults to dns.
     * If set to "stork", host will be treated as SmallRye Stork service name
     */
    @WithDefault(DNS)
    String nameResolver();

    /**
     * Whether {@code plain-text} should be used instead of {@code TLS}.
     * Enabled by default, except if TLS/SSL is configured. In this case, {@code plain-text} is disabled.
     */
    Optional<Boolean> plainText();

    /**
     * The duration after which a keep alive ping is sent.
     */
    Optional<Duration> keepAliveTime();

    /**
     * The flow control window in bytes. Default is 1MiB.
     */
    OptionalInt flowControlWindow();

    /**
     * The duration without ongoing RPCs before going to idle mode.
     */
    Optional<Duration> idleTimeout();

    /**
     * The amount of time the sender of a keep alive ping waits for an acknowledgement.
     */
    Optional<Duration> keepAliveTimeout();

    /**
     * Whether keep-alive will be performed when there are no outstanding RPC on a connection.
     */
    @WithDefault("false")
    boolean keepAliveWithoutCalls();

    /**
     * The max number of hedged attempts.
     */
    @WithDefault("5")
    int maxHedgedAttempts();

    /**
     * The max number of retry attempts.
     * Retry must be explicitly enabled.
     */
    @WithDefault("5")
    int maxRetryAttempts();

    /**
     * The maximum number of channel trace events to keep in the tracer for each channel or sub-channel.
     */
    OptionalInt maxTraceEvents();

    /**
     * The maximum message size allowed for a single gRPC frame (in bytes).
     * Default is 4 MiB.
     */
    OptionalInt maxInboundMessageSize();

    /**
     * The maximum size of metadata allowed to be received (in bytes).
     * Default is 8192B.
     */
    OptionalInt maxInboundMetadataSize();

    /**
     * The negotiation type for the HTTP/2 connection.
     * Accepted values are: {@code TLS}, {@code PLAINTEXT_UPGRADE}, {@code PLAINTEXT}
     */
    @WithDefault("TLS")
    String negotiationType();

    /**
     * Overrides the authority used with TLS and HTTP virtual hosting.
     */
    Optional<String> overrideAuthority();

    /**
     * The per RPC buffer limit in bytes used for retry.
     */
    OptionalLong perRpcBufferLimit();

    /**
     * Whether retry is enabled.
     * Note that retry is disabled by default.
     */
    @WithDefault("false")
    boolean retry();

    /**
     * The retry buffer size in bytes.
     */
    OptionalLong retryBufferSize();

    /**
     * Use a custom user-agent.
     */
    Optional<String> userAgent();

    /**
     * Use a custom load balancing policy.
     * Accepted values are: {@code pick_first}, {@code round_robin}, {@code grpclb}.
     * This value is ignored if name-resolver is set to 'stork'.
     */
    @WithDefault("pick_first")
    String loadBalancingPolicy();

    /**
     * The compression to use for each call. The accepted values are {@code gzip} and {@code identity}.
     */
    Optional<String> compression();

    /**
     * The deadline used for each call.
     */
    Optional<Duration> deadline();

    /**
     * Shared configuration for setting up client-side SSL.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @ConfigGroup
    interface SslClientConfig {
        /**
         * The classpath path or file path to a server certificate or certificate chain in PEM format.
         */
        Optional<Path> certificate();

        /**
         * The classpath path or file path to the corresponding certificate private key file in PEM format.
         */
        Optional<Path> key();

        /**
         * An optional trust store which holds the certificate information of the certificates to trust
         *
         * The trust store can be either on classpath or in an external file.
         */
        Optional<Path> trustStore();

    }

    @ConfigGroup
    interface TlsClientConfig {

        /**
         * Whether SSL/TLS is enabled.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Enable trusting all certificates. Disabled by default.
         */
        @WithDefault("false")
        boolean trustAll();

        /**
         * Trust configuration in the PEM format.
         * <p>
         * When used, {@code trust-certificate-jks} and {@code trust-certificate-p12} must not be used.
         */
        PemTrustCertConfiguration trustCertificatePem();

        /**
         * Trust configuration in the JKS format.
         * <p>
         * When configured, {@code trust-certificate-pem} and {@code trust-certificate-p12} must not be used.
         */
        JksConfiguration trustCertificateJks();

        /**
         * Trust configuration in the P12 format.
         * <p>
         * When configured, {@code trust-certificate-jks} and {@code trust-certificate-pem} must not be used.
         */
        PfxConfiguration trustCertificateP12();

        /**
         * Key/cert configuration in the PEM format.
         * <p>
         * When configured, {@code key-certificate-jks} and {@code key-certificate-p12} must not be used.
         */
        PemKeyCertConfiguration keyCertificatePem();

        /**
         * Key/cert configuration in the JKS format.
         * <p>
         * When configured, {@code #key-certificate-pem} and {@code #key-certificate-p12} must not be used.
         */
        JksConfiguration keyCertificateJks();

        /**
         * Key/cert configuration in the P12 format.
         * <p>
         * When configured, {@code key-certificate-jks} and {@code #key-certificate-pem} must not be used.
         */
        PfxConfiguration keyCertificateP12();

        /**
         * Whether hostname should be verified in the SSL/TLS handshake.
         */
        @WithDefault("true")
        boolean verifyHostname();

        @ConfigGroup
        interface PemTrustCertConfiguration {

            /**
             * Comma-separated list of the trust certificate files (Pem format).
             */
            Optional<List<String>> certs();

        }

        @ConfigGroup
        interface JksConfiguration {

            /**
             * Path of the key file (JKS format).
             */
            Optional<String> path();

            /**
             * Password of the key file.
             */
            Optional<String> password();
        }

        @ConfigGroup
        interface PfxConfiguration {

            /**
             * Path to the key file (PFX format).
             */
            Optional<String> path();

            /**
             * Password of the key.
             */
            Optional<String> password();
        }

        @ConfigGroup
        interface PemKeyCertConfiguration {

            /**
             * Comma-separated list of the path to the key files (Pem format).
             */
            Optional<List<String>> keys();

            /**
             * Comma-separated list of the path to the certificate files (Pem format).
             */
            Optional<List<String>> certs();

        }

    }

    /**
     * Client XDS config
     * * <a href="https://github.com/grpc/grpc-java/tree/master/examples/example-xds">XDS usage</a>
     */
    @ConfigGroup
    interface ClientXds extends GrpcServerConfiguration.Xds {
        /**
         * Optional explicit target.
         */
        Optional<String> target();
    }

    /**
     * Stork config for new Vert.x gRPC
     */
    @ConfigGroup
    interface StorkConfig {
        /**
         * Number of threads on a delayed gRPC ClientCall
         */
        @WithDefault("10")
        int threads();

        /**
         * Deadline in milliseconds of delayed gRPC call
         */
        @WithDefault("5000")
        long deadline();

        /**
         * Number of retries on a gRPC ClientCall
         */
        @WithDefault("3")
        int retries();

        /**
         * Initial delay in seconds on refresh check
         */
        @WithDefault("60")
        long delay();

        /**
         * Refresh period in seconds
         */
        @WithDefault("120")
        long period();
    }
}
