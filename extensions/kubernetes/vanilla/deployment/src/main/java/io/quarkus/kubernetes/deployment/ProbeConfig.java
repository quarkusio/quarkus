package io.quarkus.kubernetes.deployment;

import java.time.Duration;
import java.util.Optional;

import io.smallrye.config.WithDefault;

public interface ProbeConfig {

    /**
     * The port number to use when configuring the {@literal http get} action. If not configured, the port
     * corresponding to the {@code httpActionPortName} will be used.
     */
    Optional<Integer> httpActionPort();

    /**
     * The port name for selecting the port of the {@literal HTTP get} action.
     */
    Optional<String> httpActionPortName();

    /**
     * The http path to use for the probe. For this to work, the container port also needs to be set.
     * <p>
     * Assuming the container port has been set (as per above comment), if execAction or tcpSocketAction are not set,
     * an HTTP probe will be used automatically even if no path is set (which will result in the root path being
     * used). If Smallrye Health is used, the path will automatically be set according to the health check path.
     */
    Optional<String> httpActionPath();

    /**
     * The scheme of the {@literal HTTP get} action. Can be either "HTTP" or "HTTPS".
     */
    Optional<String> httpActionScheme();

    /**
     * The command to use for the probe.
     */
    Optional<String> execAction();

    /**
     * The tcp socket to use for the probe (the format is host:port).
     */
    Optional<String> tcpSocketAction();

    /**
     * The gRPC port to use for the probe (the format is either port or port:service).
     */
    Optional<String> grpcAction();

    /**
     * If enabled and `grpc-action` is not provided, it will use the generated service name and the gRPC port.
     */
    @WithDefault("false")
    boolean grpcActionEnabled();

    /**
     * The amount of time to wait before starting to probe.
     */
    @WithDefault("5")
    Duration initialDelay();

    /**
     * The period in which the action should be called.
     */
    @WithDefault("10s")
    Duration period();

    /**
     * The amount of time to wait for each action.
     */
    @WithDefault("10s")
    Duration timeout();

    /**
     * The success threshold to use.
     */
    @WithDefault("1")
    Integer successThreshold();

    /**
     * The failure threshold to use.
     */
    @WithDefault("3")
    Integer failureThreshold();

    default boolean hasUserSuppliedAction() {
        return httpActionPath().isPresent() || tcpSocketAction().isPresent() || execAction().isPresent()
                || grpcAction().isPresent();
    }
}
