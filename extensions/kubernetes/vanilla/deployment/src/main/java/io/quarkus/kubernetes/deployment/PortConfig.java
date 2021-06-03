
package io.quarkus.kubernetes.deployment;

import java.util.Optional;
import java.util.OptionalInt;

import io.dekorate.kubernetes.annotation.Protocol;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PortConfig {

    /**
     * The port number. Refers to the container port.
     */
    @ConfigItem
    public OptionalInt containerPort;

    /**
     * The host port.
     */
    @ConfigItem
    public OptionalInt hostPort;

    /**
     * The application path (refers to web application path).
     *
     * @return The path, defaults to /.
     */
    @ConfigItem(defaultValue = "/")
    public Optional<String> path;

    /**
     * The protocol.
     */
    @ConfigItem(defaultValue = "TCP")
    public Protocol protocol;

    /**
     * The nodePort to which this port should be mapped to.
     * This only takes affect when the serviceType is set to node-port.
     */
    public OptionalInt nodePort;

}
