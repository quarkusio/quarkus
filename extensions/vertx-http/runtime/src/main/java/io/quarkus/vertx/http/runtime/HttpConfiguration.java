package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.http")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HttpConfiguration {
    /**
     * Enable the CORS filter.
     */
    @WithName("cors.enabled")
    @WithDefault("${quarkus.http.cors:false}")
    boolean corsEnabled();

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
     *
     * In dev/test mode this defaults to localhost, in prod mode this defaults to 0.0.0.0
     *
     * Defaulting to 0.0.0.0 makes it easier to deploy Quarkus to container, however it
     * is not suitable for dev/test mode as other people on the network can connect to your
     * development machine.
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
     */
    @WithDefault("enabled")
    InsecureRequests insecureRequests();

    /**
     * If this is true (the default) then HTTP/2 will be enabled.
     *
     * Note that for browsers to be able to use it HTTPS must be enabled,
     * and you must be running on JDK11 or above, as JDK8 does not support
     * ALPN.
     */
    @WithDefault("true")
    boolean http2();

    /**
     * Enables or Disable the HTTP/2 Push feature.
     * This setting can be used to disable server push. The server will not send a {@code PUSH_PROMISE} frame if it
     * receives this parameter set to @{code false}.
     */
    @WithDefault("true")
    boolean http2PushEnabled();

    /**
     * The CORS config
     */
    CORSConfig cors();

    /**
     * The SSL config
     */
    ServerSslConfig ssl();

    /**
     * The Static Resources config
     */
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
     *
     * In general this should be controlled by setting quarkus.vertx.event-loops-pool-size, this setting should only
     * be used if you want to limit the number of HTTP io threads to a smaller number than the total number of IO threads.
     */
    OptionalInt ioThreads();

    /**
     * Server limits configuration
     */
    ServerLimitsConfig limits();

    /**
     * Http connection idle timeout
     */
    @WithName("idle-timeout")
    @WithDefault("30M")
    Duration idleTimeout();

    /**
     * Http connection read timeout for blocking IO. This is the maximum amount of time
     * a thread will wait for data, before an IOException will be thrown and the connection
     * closed.
     *
     */
    @WithName("read-timeout")
    @WithDefault("60s")
    Duration readTimeout();

    /**
     * Request body related settings
     */
    BodyConfig body();

    /**
     * The encryption key that is used to store persistent logins (e.g. for form auth). Logins are stored in a persistent
     * cookie that is encrypted with AES-256 using a key derived from a SHA-256 hash of the key that is provided here.
     *
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
     *
     * This has a small performance penalty, so is disabled by default.
     */
    @WithDefault("false")
    boolean recordRequestStartTime();

    /**
     * Access log configuration.
     */
    AccessLogConfig accessLog();

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
    Map<String, HeaderConfig> header();

    /**
     * Additional HTTP configuration per path
     */
    Map<String, FilterConfig> filter();

    /**
     * Proxy configuration.
     */
    ProxyConfig proxy();

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
    }
}
