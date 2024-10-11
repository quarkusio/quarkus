package io.quarkus.kubernetes.deployment;

import java.util.Optional;
import java.util.OptionalInt;

import io.dekorate.kubernetes.annotation.Protocol;
import io.smallrye.config.WithDefault;

public interface PortConfig {

    /**
     * The port number. Refers to the container port.
     */
    OptionalInt containerPort();

    /**
     * The host port.
     */
    OptionalInt hostPort();

    /**
     * The application path (refers to web application path).
     */
    @WithDefault("/")
    Optional<String> path();

    /**
     * The protocol.
     */
    @WithDefault("TCP")
    Protocol protocol();

    /**
     * The nodePort to which this port should be mapped to. This only takes affect when the serviceType is set to
     * node-port.
     */
    OptionalInt nodePort();

    /**
     * If enabled, the port will be configured to use the schema HTTPS.
     */
    @WithDefault("false")
    boolean tls();
}
