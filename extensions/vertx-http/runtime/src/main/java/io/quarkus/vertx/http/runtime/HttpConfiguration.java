package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class HttpConfiguration {

    /**
     * Authentication configuration
     */
    public AuthRuntimeConfig auth;

    /**
     * Enable the CORS filter.
     */
    @ConfigItem(name = "cors")
    public boolean corsEnabled;

    /**
     * The HTTP port
     */
    @ConfigItem(defaultValue = "8080")
    public int port;

    /**
     * The HTTP port used to run tests
     */
    @ConfigItem(defaultValue = "8081")
    public int testPort;

    /**
     * The HTTP host
     *
     * In dev/test mode this defaults to localhost, in prod mode this defaults to 0.0.0.0
     *
     * Defaulting to 0.0.0.0 makes it easier to deploy Quarkus to container, however it
     * is not suitable for dev/test mode as other people on the network can connect to your
     * development machine.
     */
    @ConfigItem
    public String host;

    /**
     * Used when {@code QuarkusIntegrationTest} is meant to execute against an application that is already running and
     * listening on the host specified by this property.
     */
    @ConfigItem
    public Optional<String> testHost;

    /**
     * Enable listening to host:port
     */
    @ConfigItem(defaultValue = "true")
    public boolean hostEnabled;

    /**
     * The HTTPS port
     */
    @ConfigItem(defaultValue = "8443")
    public int sslPort;

    /**
     * The HTTPS port used to run tests
     */
    @ConfigItem(defaultValue = "8444")
    public int testSslPort;

    /**
     * Used when {@code QuarkusIntegrationTest} is meant to execute against an application that is already running
     * to configure the test to use SSL.
     */
    @ConfigItem
    public Optional<Boolean> testSslEnabled;

    /**
     * If insecure (i.e. http rather than https) requests are allowed. If this is {@code enabled}
     * then http works as normal. {@code redirect} will still open the http port, but
     * all requests will be redirected to the HTTPS port. {@code disabled} will prevent the HTTP
     * port from opening at all.
     */
    @ConfigItem(defaultValue = "enabled")
    public InsecureRequests insecureRequests;

    /**
     * If this is true (the default) then HTTP/2 will be enabled.
     *
     * Note that for browsers to be able to use it HTTPS must be enabled,
     * and you must be running on JDK11 or above, as JDK8 does not support
     * ALPN.
     */
    @ConfigItem(defaultValue = "true")
    public boolean http2;

    /**
     * Enables or Disable the HTTP/2 Push feature.
     * This setting can be used to disable server push. The server will not send a {@code PUSH_PROMISE} frame if it
     * receives this parameter set to @{code false}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean http2PushEnabled;

    /**
     * The CORS config
     */
    public CORSConfig cors;

    /**
     * The SSL config
     */
    public ServerSslConfig ssl;

    /**
     * The Static Resources config
     */
    public StaticResourcesConfig staticResources;

    /**
     * When set to {@code true}, the HTTP server automatically sends `100 CONTINUE`
     * response when the request expects it (with the `Expect: 100-Continue` header).
     */
    @ConfigItem(defaultValue = "false", name = "handle-100-continue-automatically")
    public boolean handle100ContinueAutomatically;

    /**
     * The number if IO threads used to perform IO. This will be automatically set to a reasonable value based on
     * the number of CPU cores if it is not provided. If this is set to a higher value than the number of Vert.x event
     * loops then it will be capped at the number of event loops.
     *
     * In general this should be controlled by setting quarkus.vertx.event-loops-pool-size, this setting should only
     * be used if you want to limit the number of HTTP io threads to a smaller number than the total number of IO threads.
     */
    @ConfigItem
    public OptionalInt ioThreads;

    /**
     * Server limits configuration
     */
    public ServerLimitsConfig limits;

    /**
     * Http connection idle timeout
     */
    @ConfigItem(defaultValue = "30M", name = "idle-timeout")
    public Duration idleTimeout;

    /**
     * Http connection read timeout for blocking IO. This is the maximum amount of time
     * a thread will wait for data, before an IOException will be thrown and the connection
     * closed.
     *
     */
    @ConfigItem(defaultValue = "60s", name = "read-timeout")
    public Duration readTimeout;

    /**
     * Request body related settings
     */
    public BodyConfig body;

    /**
     * The encryption key that is used to store persistent logins (e.g. for form auth). Logins are stored in a persistent
     * cookie that is encrypted with AES-256 using a key derived from a SHA-256 hash of the key that is provided here.
     *
     * If no key is provided then an in-memory one will be generated, this will change on every restart though so it
     * is not suitable for production environments. This must be more than 16 characters long for security reasons
     */
    @ConfigItem(name = "auth.session.encryption-key")
    public Optional<String> encryptionKey;

    /**
     * Enable socket reuse port (linux/macOs native transport only)
     */
    @ConfigItem
    public boolean soReusePort;

    /**
     * Enable tcp quick ack (linux native transport only)
     */
    @ConfigItem
    public boolean tcpQuickAck;

    /**
     * Enable tcp cork (linux native transport only)
     */
    @ConfigItem
    public boolean tcpCork;

    /**
     * Enable tcp fast open (linux native transport only)
     */
    @ConfigItem
    public boolean tcpFastOpen;

    /**
     * The accept backlog, this is how many connections can be waiting to be accepted before connections start being rejected
     */
    @ConfigItem(defaultValue = "-1")
    public int acceptBacklog;

    /**
     * Set the SETTINGS_INITIAL_WINDOW_SIZE HTTP/2 setting.
     * Indicates the sender's initial window size (in octets) for stream-level flow control.
     * The initial value is {@code 2^16-1} (65,535) octets.
     */
    @ConfigItem
    public OptionalInt initialWindowSize;

    /**
     * Path to a unix domain socket
     */
    @ConfigItem(defaultValue = "/var/run/io.quarkus.app.socket")
    public String domainSocket;

    /**
     * Enable listening to host:port
     */
    @ConfigItem
    public boolean domainSocketEnabled;

    /**
     * If this is true then the request start time will be recorded to enable logging of total request time.
     *
     * This has a small performance penalty, so is disabled by default.
     */
    @ConfigItem
    public boolean recordRequestStartTime;

    public AccessLogConfig accessLog;

    public TrafficShapingConfig trafficShaping;

    /**
     * Configuration that allows setting the same site attributes for cookies.
     */
    @ConfigItem
    public Map<String, SameSiteCookieConfig> sameSiteCookie;

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
    @ConfigItem
    public Optional<PayloadHint> unhandledErrorContentTypeDefault;

    /**
     * Additional HTTP Headers always sent in the response
     */
    @ConfigItem
    public Map<String, HeaderConfig> header;

    /**
     * Additional HTTP configuration per path
     */
    @ConfigItem
    public Map<String, FilterConfig> filter;

    public ProxyConfig proxy;

    public int determinePort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testPort : port;
    }

    public int determineSslPort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testSslPort : sslPort;
    }

    public enum InsecureRequests {
        ENABLED,
        REDIRECT,
        DISABLED;
    }

    public enum PayloadHint {
        JSON,
        HTML,
    }
}
