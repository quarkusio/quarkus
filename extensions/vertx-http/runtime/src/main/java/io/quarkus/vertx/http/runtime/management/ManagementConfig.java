package io.quarkus.vertx.http.runtime.management;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;
import static io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils.RANDOM_PORT_MANAGEMENT;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.VertxConfigBuilder;
import io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * Partial mapping of <code>quarkus.management</code> to help with the assignment of random ports in
 * {@link VertxConfigBuilder}. Can be removed and replaced when {@link ManagementInterfaceConfiguration}
 * moves to {@link ConfigMapping}.
 */
@ConfigMapping(prefix = "quarkus.management")
@ConfigRoot(phase = RUN_TIME)
public interface ManagementConfig {
    @ConfigDocIgnore
    @WithDefault("9000")
    int port();

    @ConfigDocIgnore
    @WithDefault("9001")
    int testPort();

    default int determinePort(LaunchMode mode) {
        int port = mode.equals(LaunchMode.TEST) ? testPort() : port();
        return port == 0 ? RANDOM_PORT_MANAGEMENT : port;
    }

    /**
     * In {@link ManagementInterfaceConfiguration#host} this is mapped as an {@link Optional}, but when used it
     * immediately fallbacks to <code>0.0.0.0</code>.
     *
     * @see HttpServerOptionsUtils#applyCommonOptionsForManagementInterface
     */
    @ConfigDocIgnore
    @WithDefault("0.0.0.0")
    String host();

    @ConfigDocIgnore
    @WithDefault("true")
    boolean hostEnabled();

    @ConfigDocIgnore
    @WithParentName
    Map<String, String> properties();
}
