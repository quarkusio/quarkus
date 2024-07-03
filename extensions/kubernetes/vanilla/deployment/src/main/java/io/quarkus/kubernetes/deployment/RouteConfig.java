package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RouteConfig {

    /**
     * If true, the service will be exposed
     */
    @ConfigItem
    boolean expose;

    /**
     * The host under which the application is going to be exposed
     */
    @ConfigItem
    Optional<String> host;

    /**
     * The target named port. If not provided, it will be deducted from the Service resource ports.
     * Options are: "http" and "https".
     */
    @ConfigItem(defaultValue = "http")
    String targetPort;

    /**
     * Custom annotations to add to exposition (route or ingress) resources
     */
    @ConfigItem
    @ConfigDocMapKey("annotation-name")
    Map<String, String> annotations;

    /**
     * Custom labels to add to exposition (route or ingress) resources
     */
    @ConfigItem
    @ConfigDocMapKey("label-name")
    Map<String, String> labels;

    /**
     * The TLS configuration for the route.
     */
    TLSConfig tls;
}
