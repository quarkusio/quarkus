package io.quarkus.grpc.runtime.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.vertx.core.http.ClientAuth;

@ConfigGroup
public interface GrpcServerConfiguration {

    /**
     * Do we use separate HTTP server to serve gRPC requests.
     * Set this to false if you want to use new Vert.x gRPC support,
     * which uses existing Vert.x HTTP server.
     */
    @WithDefault("true")
    boolean useSeparateServer();

    /**
     * Configure XDS usage, if enabled.
     */
    @ConfigDocSection(generated = true)
    Xds xds();

    /**
     * Configure InProcess usage, if enabled.
     */
    InProcess inProcess();

    /**
     * The gRPC Server port.
     */
    @WithDefault("9000")
    int port();

    /**
     * The gRPC Server port used for tests.
     */
    @WithDefault("9001")
    int testPort();

    /**
     * The gRPC server host.
     */
    @WithDefault("0.0.0.0")
    String host();

    /**
     * The gRPC handshake timeout.
     */
    Optional<Duration> handshakeTimeout();

    /**
     * The max inbound message size in bytes.
     * <p>
     * When using a single server (using {@code quarkus.grpc.server.use-separate-server=false}), the default value is 256KB.
     * When using a separate server (using {@code quarkus.grpc.server.use-separate-server=true}), the default value is 4MB.
     */
    OptionalInt maxInboundMessageSize();

    /**
     * The max inbound metadata size in bytes
     */
    OptionalInt maxInboundMetadataSize();

    /**
     * The SSL/TLS config.
     */
    SslServerConfig ssl();

    /**
     * Disables SSL, and uses plain text instead.
     * If disabled, configure the ssl configuration.
     */
    @WithDefault("true")
    boolean plainText();

    default boolean isPlainTextEnabled() {
        boolean plainText = plainText();
        if (plainText && (ssl().certificate().isPresent() || ssl().keyStore().isPresent())) {
            plainText = false;
        }
        return plainText;
    }

    /**
     * Whether ALPN should be used.
     */
    @WithDefault("true")
    boolean alpn();

    /**
     * Configures the transport security.
     */
    GrpcTransportSecurity transportSecurity();

    /**
     * Enables the gRPC Reflection Service.
     * By default, the reflection service is only exposed in `dev` mode.
     * This setting allows overriding this choice and enable the reflection service every time.
     */
    @WithDefault("false")
    boolean enableReflectionService();

    /**
     * Number of gRPC server verticle instances.
     * This is useful for scaling easily across multiple cores.
     * The number should not exceed the amount of event loops.
     */
    @WithDefault("1")
    int instances();

    /**
     * Configures the netty server settings.
     */
    GrpcServerNettyConfig netty();

    /**
     * gRPC compression, e.g. "gzip"
     */
    Optional<String> compression();

    /**
     * Shared configuration for setting up server-side SSL.
     */
    @ConfigGroup
    public interface SslServerConfig {
        /**
         * The classpath path or file path to a server certificate or certificate chain in PEM format.
         */
        Optional<Path> certificate();

        /**
         * The classpath path or file path to the corresponding certificate private key file in PEM format.
         */
        Optional<Path> key();

        /**
         * An optional keystore that holds the certificate information instead of specifying separate files.
         * The keystore can be either on classpath or an external file.
         */
        Optional<Path> keyStore();

        /**
         * An optional parameter to specify the type of the keystore file. If not given, the type is automatically detected
         * based on the file name.
         */
        Optional<String> keyStoreType();

        /**
         * A parameter to specify the password of the keystore file.
         */
        Optional<String> keyStorePassword();

        /**
         * A parameter to specify the alias of the keystore file.
         */
        Optional<String> keyStoreAlias();

        /**
         * A parameter to specify the alias password of the keystore file.
         */
        Optional<String> keyStoreAliasPassword();

        /**
         * An optional trust store which holds the certificate information of the certificates to trust
         * <p>
         * The trust store can be either on classpath or an external file.
         */
        Optional<Path> trustStore();

        /**
         * An optional parameter to specify type of the trust store file. If not given, the type is automatically detected
         * based on the file name.
         */
        Optional<String> trustStoreType();

        /**
         * A parameter to specify the password of the trust store file.
         */
        Optional<String> trustStorePassword();

        /**
         * The cipher suites to use. If none is given, a reasonable default is selected.
         */
        Optional<List<String>> cipherSuites();

        /**
         * Sets the ordered list of enabled SSL/TLS protocols.
         * <p>
         * If not set, it defaults to {@code "TLSv1.3, TLSv1.2"}.
         * The following list of protocols are supported: {@code TLSv1, TLSv1.1, TLSv1.2, TLSv1.3}.
         * To only enable {@code TLSv1.3}, set the value to {@code to "TLSv1.3"}.
         * <p>
         * Note that setting an empty list, and enabling SSL/TLS is invalid.
         * You must at least have one protocol.
         */
        @WithDefault("TLSv1.3,TLSv1.2")
        Set<String> protocols();

        /**
         * Configures the engine to require/request client authentication.
         * NONE, REQUEST, REQUIRED
         */
        @WithDefault("NONE")
        ClientAuth clientAuth();
    }

    @ConfigGroup
    public interface GrpcServerNettyConfig {

        /**
         * Sets a custom keep-alive duration. This configures the time before sending a `keepalive` ping
         * when there is no read activity.
         */
        Optional<Duration> keepAliveTime();

        /**
         * Sets a custom permit-keep-alive duration. This configures the most aggressive keep-alive time clients
         * are permitted to configure.
         * The server will try to detect clients exceeding this rate and when detected will forcefully close the connection.
         *
         * @see #permitKeepAliveWithoutCalls
         */
        Optional<Duration> permitKeepAliveTime();

        /**
         * Sets whether to allow clients to send keep-alive HTTP/2 PINGs even if
         * there are no outstanding RPCs on the connection.
         */
        Optional<Boolean> permitKeepAliveWithoutCalls();

    }

    @ConfigGroup
    public interface GrpcTransportSecurity {

        /**
         * The path to the certificate file.
         */
        Optional<String> certificate();

        /**
         * The path to the private key file.
         */
        Optional<String> key();
    }

    /**
     * XDS config
     * * <a href="https://github.com/grpc/grpc-java/tree/master/examples/example-xds">XDS usage</a>
     */
    @ConfigGroup
    interface Xds extends Enabled {
        /**
         * Explicitly enable use of XDS.
         */
        @WithDefault("false")
        @Override
        boolean enabled();

        /**
         * Use secure credentials.
         */
        @WithDefault("false")
        boolean secure();
    }
}
