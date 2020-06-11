package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.OptionalInt;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class HttpConfiguration {

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
     */
    @ConfigItem(defaultValue = "0.0.0.0")
    public String host;

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
     * If this is true then the address, scheme etc will be set from headers forwarded by the proxy server, such as
     * {@code X-Forwarded-For}. This should only be set if you are behind a proxy that sets these headers.
     */
    @ConfigItem
    public boolean proxyAddressForwarding;

    /**
     * If this is true and proxy address forwarding is enabled then the standard {@code Forwarded} header will be used,
     * rather than the more common but not standard {@code X-Forwarded-For}.
     */
    @ConfigItem
    public boolean allowForwarded;

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
     * The CORS config
     */
    public CORSConfig cors;

    /**
     * The SSL config
     */
    public ServerSslConfig ssl;

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
     * The session token config. Used for persistent login if JWT support is not present
     */
    @ConfigItem(name = "auth.session")
    public AuthSessionConfig sessionConfig;

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

    AccessLogConfig accessLog;

    /**
     * Configuration that allows setting the same site attributes for cookies.
     */
    @ConfigItem
    public Map<String, SameSiteCookieConfig> sameSiteCookie;

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
}
