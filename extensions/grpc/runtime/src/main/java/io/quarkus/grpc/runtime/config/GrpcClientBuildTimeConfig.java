package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class GrpcClientBuildTimeConfig {

    /**
     * If set to true, and a Stork load balancer is used, connections with all available service instances will be
     * requested proactively. This means better load balancing at the cost of having multiple active connections.
     */
    @ConfigItem(defaultValue = "true")
    public boolean storkProactiveConnections;
}
