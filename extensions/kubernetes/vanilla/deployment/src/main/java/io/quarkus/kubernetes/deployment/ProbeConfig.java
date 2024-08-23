
package io.quarkus.kubernetes.deployment;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ProbeConfig {

    /**
     * The port number to use when configuring the {@literal http get} action.
     * If not configured, the port corresponding to the {@code httpActionPortName} will be used.
     */
    @ConfigItem
    Optional<Integer> httpActionPort;

    /**
     * The port name for selecting the port of the {@literal HTTP get} action.
     */
    @ConfigItem
    Optional<String> httpActionPortName;

    /**
     * The http path to use for the probe. For this to work, the container port also
     * needs to be set.
     *
     * Assuming the container port has been set (as per above comment), if
     * execAction or tcpSocketAction are not set, an HTTP probe will be used
     * automatically even if no path is set (which will result in the root path
     * being used).
     * If Smallrye Health is used, the path will automatically be set according to the health check path.
     */
    @ConfigItem
    Optional<String> httpActionPath;

    /**
     * The scheme of the {@literal HTTP get} action. Can be either "HTTP" or "HTTPS".
     */
    @ConfigItem
    Optional<String> httpActionScheme;

    /**
     * The command to use for the probe.
     */
    @ConfigItem
    Optional<String> execAction;

    /**
     * The tcp socket to use for the probe (the format is host:port).
     */
    @ConfigItem
    Optional<String> tcpSocketAction;

    /**
     * The gRPC port to use for the probe (the format is either port or port:service).
     */
    @ConfigItem
    Optional<String> grpcAction;

    /**
     * If enabled and `grpc-action` is not provided, it will use the generated service name and the gRPC port.
     */
    @ConfigItem(defaultValue = "false")
    boolean grpcActionEnabled;

    /**
     * The amount of time to wait before starting to probe.
     */
    @ConfigItem(defaultValue = "5")
    Duration initialDelay;

    /**
     * The period in which the action should be called.
     */
    @ConfigItem(defaultValue = "10s")
    Duration period;

    /**
     * The amount of time to wait for each action.
     */
    @ConfigItem(defaultValue = "10s")
    Duration timeout;

    /**
     * The success threshold to use.
     */
    @ConfigItem(defaultValue = "1")
    Integer successThreshold;

    /**
     * The failure threshold to use.
     */
    @ConfigItem(defaultValue = "3")
    Integer failureThreshold;

    public boolean hasUserSuppliedAction() {
        return httpActionPath.isPresent() || tcpSocketAction.isPresent() || execAction.isPresent() || grpcAction.isPresent();
    }
}
