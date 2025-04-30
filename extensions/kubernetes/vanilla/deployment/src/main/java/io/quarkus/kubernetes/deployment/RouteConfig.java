package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;

public interface RouteConfig {

    /**
     * If true, the service will be exposed
     */
    @WithDefault("false")
    boolean expose();

    /**
     * The host under which the application is going to be exposed
     */
    Optional<String> host();

    /**
     * The target named port. If not provided, it will be deducted from the Service resource ports.
     * Options are: "http" and "https".
     */
    @WithDefault("http")
    String targetPort();

    /**
     * Custom annotations to add to exposition (route or ingress) resources
     */
    @ConfigDocMapKey("annotation-name")
    Map<String, String> annotations();

    /**
     * Custom labels to add to exposition (route or ingress) resources
     */
    @ConfigDocMapKey("label-name")
    Map<String, String> labels();

    /**
     * The TLS configuration for the route.
     */
    TLSConfig tls();
}
