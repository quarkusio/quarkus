package io.quarkus.vertx.http.runtime;

import static io.quarkus.runtime.LaunchMode.TEST;
import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;
import static io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils.RANDOM_PORT_MAIN_HTTP;
import static io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils.RANDOM_PORT_MAIN_TLS;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * Partial mapping of <code>quarkus.http</code> to help with the assignment of random ports in
 * {@link VertxConfigBuilder}. Can be removed and replaced when {@link HttpConfiguration} moves to
 * {@link ConfigMapping}.
 */
@ConfigMapping(prefix = "quarkus.http")
@ConfigRoot(phase = RUN_TIME)
public interface HttpConfig {
    @ConfigDocIgnore
    @WithDefault("8080")
    int port();

    @ConfigDocIgnore
    @WithDefault("8081")
    int testPort();

    default int determinePort(LaunchMode mode) {
        int port = mode.equals(TEST) ? testPort() : port();
        return port == 0 ? RANDOM_PORT_MAIN_HTTP : port;
    }

    @ConfigDocIgnore
    String host();

    @ConfigDocIgnore
    Optional<String> testHost();

    @ConfigDocIgnore
    @WithDefault("true")
    boolean hostEnabled();

    @ConfigDocIgnore
    @WithDefault("8443")
    int sslPort();

    default int determineSslPort(LaunchMode launchMode) {
        int sslPort = launchMode.equals(TEST) ? testSslPort() : sslPort();
        return sslPort == 0 ? RANDOM_PORT_MAIN_TLS : sslPort;
    }

    @ConfigDocIgnore
    @WithDefault("8444")
    int testSslPort();

    @ConfigDocIgnore
    @WithParentName
    Map<String, String> properties();
}
