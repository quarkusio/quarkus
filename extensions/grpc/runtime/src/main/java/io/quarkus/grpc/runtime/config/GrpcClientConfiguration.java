package io.quarkus.grpc.runtime.config;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface GrpcClientConfiguration {

    String DNS = "dns";

    /**
     * Configure Stork usage with new Vert.x gRPC, if enabled.
     */
    StorkConfig stork();

    /**
     * The gRPC service port.
     */
    @WithDefault("8080")
    int port();

    /**
     * The gRPC service test port.
     * If not set, uses 8081 for plain text and 8444 when TLS is used.
     */
    OptionalInt testPort();

    /**
     * The host name / IP on which the service is exposed.
     */
    @WithDefault("localhost")
    String host();

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
     * The maximum message size allowed for a single gRPC frame (in bytes).
     * Default is 4 MiB.
     */
    OptionalInt maxInboundMessageSize();

    /**
     * The compression to use for each call. The accepted values are {@code gzip} and {@code identity}.
     */
    Optional<String> compression();

    /**
     * The deadline used for each call.
     */
    Optional<Duration> deadline();

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
