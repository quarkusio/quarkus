package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

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
     */
    @ConfigItem
    Optional<String> targetPort;

    /**
     * Custom annotations to add to exposition (route or ingress) resources
     */
    @ConfigItem
    Map<String, String> annotations;

}
