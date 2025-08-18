package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.http")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface VertxHttpConfig {
    /**
     * Authentication configuration
     */
    @ConfigDocSection(generated = true)
    AuthRuntimeConfig auth();

    /**
     * Enable the CORS filter.
     *
     * @deprecated Use {@link CORSConfig#enabled()}. Deprecated because it requires additional syntax to
     *             configure with the group {@link VertxHttpConfig#cors()} in YAML config.
     */
    @WithName("cors")
    @WithDefault("false")
    @Deprecated
    boolean oldCorsEnabled();

    /**
     * The HTTP port
     */
    @WithDefault("8080")
    int port();

    /**
     * The HTTP port used to run tests
     */
    @WithDefault("8081")
    int testPort();

    /**
     * The HTTP host
     * <p>
     * In dev/test mode this defaults to localhost, in prod mode this defaults to 0.0.0.0
     * <p>
     * Defaulting to 0.0.0.0 makes it easier to deploy Quarkus to container, however it
     * is not suitable for dev/test mode as other people on the network can connect to your
     * development machine.
     * <p>
     * As an exception, when running in Windows Subsystem for Linux (WSL), the HTTP host
     * defaults to 0.0.0.0 even in dev/test mode since using localhost makes the application
     * inaccessible.
     */
    String host();

    /**
     * Used when {@code QuarkusIntegrationTest} is meant to execute against an application that is already running and
     * listening on the host specified by this property.
     */
    Optional<String> testHost();

    /**
     * Enable listening to host:port
     */
    @WithDefault("true")
    boolean hostEnabled();

    /**
     * The HTTPS port
     */
    @WithDefault("8443")
    int sslPort();

    /**
     * The HTTPS port used to run tests
     */
    @WithDefault("8444")
    int testSslPort();

    /**
     * Used when {@code QuarkusIntegrationTest} is meant to execute against an application that is already running
     * to configure the test to use SSL.
     */
    Optional<Boolean> testSslEnabled();

    /**
     * If insecure (i.e. http rather than https) requests are allowed. If this is {@code enabled}
     * then http works as normal. {@code redirect} will still open the http port, but
     * all requests will be redirected to the HTTPS port. {@code disabled} will prevent the HTTP
     * port from opening at all.
     * <p>
     * Default is {@code enabled} except when client auth is set to {@code required} (configured using
     * {@code quarkus.http.ssl.client-auth=required}).
     * In this case, the default is {@code disabled}.
     */
    Optional<InsecureRequests> insecureRequests();

    /**
     * If this is true (the default) then HTTP/2 will be enabled.
     * <p>
     * Note that for browsers to be able to use it HTTPS must be enabled.
     */
    @WithDefault("true")
    boolean http2();

    /**
     * Enables or Disable the HTTP/2 Push feature.
     * This setting can be used to disable server push. The server will not send a {@code PUSH_PROMISE} frame if it
     * receives this parameter set to {@code false}.
     */
    @WithDefault("true")
    boolean http2PushEnabled();

    /**
     * The CORS config
     */
    @ConfigDocSection(generated = true)
    CORSConfig cors();

    /**
     * The SSL config
     */
    ServerSslConfig ssl();

    /**
     * The name of the TLS configuration to use.
     * <p>
     * If not set and the default TLS configuration is configured ({@code quarkus.tls.*}) then that will be used.
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     * <p>
     * If no TLS configuration is set, and {@code quarkus.tls.*} is not configured, then, `quarkus.http.ssl` will be used.
     */
    Optional<String> tlsConfigurationName();

    /**
     * Static Resources.
     */
    @ConfigDocSection(generated = true)
    StaticResourcesConfig staticResources();

    /**
     * When set to {@code true}, the HTTP server automatically sends `100 CONTINUE`
     * response when the request expects it (with the `Expect: 100-Continue` header).
     */
    @WithName("handle-100-continue-automatically")
    @WithDefault("false")
    boolean handle100ContinueAutomatically();

    /**
     * The number if IO threads used to perform IO. This will be automatically set to a reasonable value based on
     * the number of CPU cores if it is not provided. If this is set to a higher value than the number of Vert.x event
     * loops then it will be capped at the number of event loops.
     * <p>
     * In general this should be controlled by setting quarkus.vertx.event-loops-pool-size, this setting should only
     * be used if you want to limit the number of HTTP io threads to a smaller number than the total number of IO threads.
     */
    OptionalInt ioThreads();

    /**
     * Server limits.
     */
    @ConfigDocSection(generated = true)
    ServerLimitsConfig limits();

    /**
     * Http connection idle timeout
     */
    @WithDefault("30M")
    Duration idleTimeout();

    /**
     * Http connection read timeout for blocking IO. This is the maximum amount of time
     * a thread will wait for data, before an IOException will be thrown and the connection
     * closed.
     */
    @WithDefault("60s")
    Duration readTimeout();

    /**
     * Request body related settings
     */
    BodyConfig body();

    /**
     * The encryption key that is used to store persistent logins (e.g. for form auth). Logins are stored in a persistent
     * cookie that is encrypted with AES-256 using a key derived from a SHA-256 hash of the key that is provided here.
     * <p>
     * If no key is provided then an in-memory one will be generated, this will change on every restart though so it
     * is not suitable for production environments. This must be more than 16 characters long for security reasons
     */
    @WithName("auth.session.encryption-key")
    Optional<String> encryptionKey();

    /**
     * Enable socket reuse port (linux/macOs native transport only)
     */
    @WithDefault("false")
    boolean soReusePort();

    /**
     * Enable tcp quick ack (linux native transport only)
     */
    @WithDefault("false")
    boolean tcpQuickAck();

    /**
     * Enable tcp cork (linux native transport only)
     */
    @WithDefault("false")
    boolean tcpCork();

    /**
     * Enable tcp fast open (linux native transport only)
     */
    @WithDefault("false")
    boolean tcpFastOpen();

    /**
     * The accept backlog, this is how many connections can be waiting to be accepted before connections start being rejected
     */
    @WithDefault("-1")
    int acceptBacklog();

    /**
     * Set the SETTINGS_INITIAL_WINDOW_SIZE HTTP/2 setting.
     * Indicates the sender's initial window size (in octets) for stream-level flow control.
     * The initial value is {@code 2^16-1} (65,535) octets.
     */
    OptionalInt initialWindowSize();

    /**
     * Path to a unix domain socket
     */
    @WithDefault("/var/run/io.quarkus.app.socket")
    String domainSocket();

    /**
     * Enable listening to host:port
     */
    @WithDefault("false")
    boolean domainSocketEnabled();

    /**
     * If this is true then the request start time will be recorded to enable logging of total request time.
     * <p>
     * This has a small performance penalty, so is disabled by default.
     */
    @WithDefault("false")
    boolean recordRequestStartTime();

    /**
     * Access logs.
     */
    @ConfigDocSection(generated = true)
    AccessLogConfig accessLog();

    /**
     * Traffic shaping.
     */
    @ConfigDocSection
    TrafficShapingConfig trafficShaping();

    /**
     * Configuration that allows setting the same site attributes for cookies.
     */
    Map<String, SameSiteCookieConfig> sameSiteCookie();

    /**
     * Provides a hint (optional) for the default content type of responses generated for
     * the errors not handled by the application.
     * <p>
     * If the client requested a supported content-type in request headers
     * (e.g. "Accept: application/json", "Accept: text/html"),
     * Quarkus will use that content type.
     * <p>
     * Otherwise, it will default to the content type configured here.
     * </p>
     */
    Optional<PayloadHint> unhandledErrorContentTypeDefault();

    /**
     * Additional HTTP Headers always sent in the response
     */
    @ConfigDocSection(generated = true)
    Map<String, HeaderConfig> header();

    /**
     * Additional HTTP configuration per path
     */
    @ConfigDocSection(generated = true)
    Map<String, FilterConfig> filter();

    /**
     * Proxy.
     */
    @ConfigDocSection
    ProxyConfig proxy();

    /**
     * WebSocket Server configuration.
     */
    @ConfigDocSection
    WebsocketServerConfig websocketServer();

    default int determinePort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testPort() : port();
    }

    default int determineSslPort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testSslPort() : sslPort();
    }

    enum InsecureRequests {
        ENABLED,
        REDIRECT,
        DISABLED;
    }

    enum PayloadHint {
        JSON,
        HTML,
        TEXT
    }
}
