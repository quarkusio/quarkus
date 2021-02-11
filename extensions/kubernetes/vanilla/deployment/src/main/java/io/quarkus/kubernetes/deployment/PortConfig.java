
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
    OptionalInt containerPort;

    /**
     * The host port.
     */
    @ConfigItem
    OptionalInt hostPort;

    /**
     * The application path (refers to web application path).
     *
     * @return The path, defaults to /.
     */
    @ConfigItem(defaultValue = "/")
    Optional<String> path;

    /**
     * The protocol.
     */
    @ConfigItem(defaultValue = "TCP")
    Protocol protocol;

}
