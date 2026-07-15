package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.vertx.core.http.Http2Settings;

@ConfigMapping(prefix = "quarkus.http")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface VertxHttpConfig {
    /**
     * Authentication configuration
     */
    @ConfigDocSection(generated = true)
    AuthRuntimeConfig auth();

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
     * The HTTPS host.
     * <p>
     * When not set, defaults to the value of {@link #host()}.
     * <p>
     * This allows binding plain HTTP and HTTPS to different network interfaces. For example, HTTP can listen on
     * {@code 127.0.0.1} for local health checks while HTTPS listens on {@code 0.0.0.0} for remote clients.
     */
    Optional<String> sslHost();

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
     * Set the default HTTP/2 connection window size. It overrides the initial window
     * size set by {@link Http2Settings#getInitialWindowSize}, so the connection window size
     * is greater than for its streams, in order the data throughput.
     * <p/>
     * A value of {@code -1} reuses the initial window size setting.
     */
    @ConfigDocDefault("-1")
    OptionalInt http2ConnectionWindowSize();

    /**
     * The CORS config
     */
    @ConfigDocSection(generated = true)
    CORSConfig cors();

    /**
     * The HTTP Host header validation
     */
    @ConfigDocSection(generated = true)
    HostValidationConfig hostValidation();

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
     * Whether the HTTP server should treat the semicolon ({@code ;}) as a query parameter
     * delimiter, in addition to the ampersand ({@code &}).
     * <p>
     * When set to {@code true} (the default), a request like {@code /path?a=1;b=2} is parsed
     * as two parameters ({@code a=1} and {@code b=2}). When set to {@code false}, the
     * semicolon is treated as a literal character and the request yields a single parameter
     * ({@code a=1;b=2}).
     * <p>
     * The default is {@code true} to preserve backward compatibility. It will change to
     * {@code false} in Quarkus 4, as using the semicolon as a query parameter delimiter is
     * uncommon and can cause issues when semicolons appear as part of parameter values.
     */
    @WithDefault("true")
    boolean useSemicolonAsQueryParamDelimiter();

    /**
     * TCP user timeout (linux native transport only). 0 means disabled.
     */
    @WithDefault("0s")
    Duration tcpUserTimeout();

    /**
     * Socket linger timeout in seconds. -1 means disabled.
     */
    @WithDefault("-1")
    int soLinger();

    /**
     * Socket send buffer size. When not set, the OS default is used.
     */
    OptionalInt sendBufferSize();

    /**
     * Socket receive buffer size. When not set, the OS default is used.
     */
    OptionalInt receiveBufferSize();

    /**
     * Read-specific idle timeout. 0 means disabled.
     * <p>
     * Unlike the general {@link #idleTimeout()}, this timeout only considers read activity.
     */
    @WithDefault("0s")
    Duration readIdleTimeout();

    /**
     * Write-specific idle timeout. 0 means disabled.
     * <p>
     * Unlike the general {@link #idleTimeout()}, this timeout only considers write activity.
     */
    @WithDefault("0s")
    Duration writeIdleTimeout();

    /**
     * Minimum response body size (in bytes) to trigger compression.
     * 0 means compress everything when compression is enabled.
     */
    @WithDefault("0")
    int compressionContentSizeThreshold();

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
     * The timeout for the PROXY protocol handshake.
     * When the PROXY protocol is enabled ({@code quarkus.http.proxy.use-proxy-protocol=true}),
     * this configures how long the server waits for the PROXY protocol header before closing the connection.
     */
    @WithDefault("10s")
    Duration proxyProtocolTimeout();

    /**
     * The maximum number of small HTTP/2 CONTINUATION frames allowed per request before the connection
     * is closed. This protects against HTTP/2 CONTINUATION flood attacks.
     * Setting zero or a negative value disables the protection.
     */
    OptionalInt http2MaxSmallContinuationFrames();

    /**
     * The initial buffer size for the HTTP/1.x decoder.
     * <p>
     * This is an advanced setting. The initial buffer size used by the HTTP/1.x decoder.
     * A larger buffer can improve performance when processing large headers but increases memory usage.
     */
    @WithDefault("128")
    int decoderInitialBufferSize();

    /**
     * Enable TCP keepalive.
     * <p>
     * This is an advanced setting. When enabled, the server sends TCP keepalive probes to detect dead connections.
     */
    @WithDefault("false")
    boolean tcpKeepAlive();

    /**
     * Enable Netty-level wire logging for the HTTP server.
     * <p>
     * This is an advanced setting. When enabled, all bytes sent and received on the server are logged
     * at DEBUG level using the Netty logging handler. This produces a large volume of logs.
     */
    @WithDefault("false")
    boolean logActivity();

    /**
     * The format for Netty activity log data. Only used when {@code log-activity} is enabled.
     * <p>
     * This is an advanced setting.
     */
    @WithDefault("HEX_DUMP")
    ActivityLogDataFormat activityLogDataFormat();

    /**
     * Enable address reuse on the server socket.
     * <p>
     * This is an advanced setting. When enabled, the {@code SO_REUSEADDR} option is set on the server socket.
     */
    @WithDefault("true")
    boolean reuseAddress();

    /**
     * The value of the IP traffic class (TOS field) for outgoing packets.
     * <p>
     * This is an advanced setting. Valid values range from 0 to 255.
     * A value of {@code -1} (the default) means the traffic class is not set.
     */
    @WithDefault("-1")
    int trafficClass();

    /**
     * Path to a Unix Domain Socket.
     */
    @WithDefault("/var/run/io.quarkus.app.socket")
    String domainSocket();

    enum ActivityLogDataFormat {
        HEX_DUMP,
        SIMPLE
    }

    /**
     * Enable listening on a Unix Domain Socket.
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

    default String determineSslHost() {
        return sslHost().orElse(host());
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
