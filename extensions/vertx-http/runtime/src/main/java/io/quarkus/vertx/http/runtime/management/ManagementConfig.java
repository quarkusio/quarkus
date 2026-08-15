package io.quarkus.vertx.http.runtime.management;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.BodyConfig;
import io.quarkus.vertx.http.runtime.FilterConfig;
import io.quarkus.vertx.http.runtime.HeaderConfig;
import io.quarkus.vertx.http.runtime.HostValidationConfig;
import io.quarkus.vertx.http.runtime.ProxyConfig;
import io.quarkus.vertx.http.runtime.ServerLimitsConfig;
import io.quarkus.vertx.http.runtime.ServerSslConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.WebsocketServerConfig;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Management interface.
 * <p>
 * Note that the management interface must be enabled using the
 * {@link ManagementInterfaceBuildTimeConfig#enabled} build-time property.
 */
@ConfigMapping(prefix = "quarkus.management")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ManagementConfig {
    /**
     * Authentication configuration
     */
    ManagementRuntimeAuthConfig auth();

    /**
     * The HTTP port
     */
    @WithDefault("9000")
    int port();

    /**
     * The HTTP port
     */
    @WithDefault("9001")
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
     * Enable listening to host:port
     */
    @WithDefault("true")
    boolean hostEnabled();

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
     * If no TLS configuration is set, and {@code quarkus.tls.*} is not configured, then, `quarkus.management.ssl` will be used.
     */
    Optional<String> tlsConfigurationName();

    /**
     * When set to {@code true}, the HTTP server automatically sends `100 CONTINUE`
     * response when the request expects it (with the `Expect: 100-Continue` header).
     */
    @WithName("handle-100-continue-automatically")
    @WithDefault("false")
    boolean handle100ContinueAutomatically();

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
     * Server limits configuration
     */
    ServerLimitsConfig limits();

    /**
     * Http connection idle timeout
     */
    @WithDefault("30M")
    Duration idleTimeout();

    /**
     * Request body related settings
     */
    BodyConfig body();

    /**
     * The accept backlog, this is how many connections can be waiting to be accepted before connections start being rejected
     */
    @WithDefault("-1")
    int acceptBacklog();

    /**
     * Path to a Unix Domain Socket.
     */
    @WithDefault("/var/run/io.quarkus.management.socket")
    String domainSocket();

    /**
     * Enable listening to host:port
     */
    @WithDefault("false")
    boolean domainSocketEnabled();

    /**
     * Additional HTTP Headers always sent in the response
     */
    Map<String, HeaderConfig> header();

    /**
     * Additional HTTP configuration per path
     */
    Map<String, FilterConfig> filter();

    /**
     * Holds configuration related with proxy addressing forward.
     */
    ProxyConfig proxy();

    /**
     * WebSocket Server configuration.
     */
    @ConfigDocSection
    WebsocketServerConfig websocketServer();

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
     */
    @WithDefault("0s")
    Duration readIdleTimeout();

    /**
     * Write-specific idle timeout. 0 means disabled.
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
     * The timeout for the PROXY protocol handshake.
     * When the PROXY protocol is enabled ({@code quarkus.management.proxy.use-proxy-protocol=true}),
     * this configures how long the server waits for the PROXY protocol header before closing the connection.
     */
    @WithDefault("10s")
    Duration proxyProtocolTimeout();

    /**
     * Enable TCP keepalive.
     * <p>
     * This is an advanced setting. When enabled, the server sends TCP keepalive probes to detect dead connections.
     */
    @WithDefault("false")
    boolean tcpKeepAlive();

    /**
     * Enable Netty-level wire logging for the management interface.
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
    VertxHttpConfig.ActivityLogDataFormat activityLogDataFormat();

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

    default int determinePort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testPort() : port();
    }
}
