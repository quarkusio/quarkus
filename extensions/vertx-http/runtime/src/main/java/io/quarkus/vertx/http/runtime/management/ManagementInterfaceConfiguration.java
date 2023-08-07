package io.quarkus.vertx.http.runtime.management;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.BodyConfig;
import io.quarkus.vertx.http.runtime.FilterConfig;
import io.quarkus.vertx.http.runtime.HeaderConfig;
import io.quarkus.vertx.http.runtime.ProxyConfig;
import io.quarkus.vertx.http.runtime.ServerLimitsConfig;
import io.quarkus.vertx.http.runtime.ServerSslConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configures the management interface.
 * Note that the management interface must be enabled using the
 * {@link ManagementInterfaceBuildTimeConfig#enabled} build-time property.
 */
@ConfigMapping(prefix = "quarkus.management")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ManagementInterfaceConfiguration {
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
     *
     * Defaults to 0.0.0.0
     *
     * Defaulting to 0.0.0.0 makes it easier to deploy Quarkus to container, however it
     * is not suitable for dev/test mode as other people on the network can connect to your
     * development machine.
     */
    Optional<String> host();

    /**
     * Enable listening to host:port
     */
    @WithDefault("true")
    boolean hostEnabled();

    /**
     * The SSL config
     */
    ServerSslConfig ssl();

    /**
     * When set to {@code true}, the HTTP server automatically sends `100 CONTINUE`
     * response when the request expects it (with the `Expect: 100-Continue` header).
     */
    @WithName("handle-100-continue-automatically")
    @WithDefault("false")
    boolean handle100ContinueAutomatically();

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
     * Request body related settings
     */
    BodyConfig body();

    /**
     * The accept backlog, this is how many connections can be waiting to be accepted before connections start being rejected
     */
    @WithDefault("-1")
    int acceptBacklog();

    /**
     * Path to a unix domain socket
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
     * Proxy configuration.
     */
    ProxyConfig proxy();

    default int determinePort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testPort() : port();
    }
}
