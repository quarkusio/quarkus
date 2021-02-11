package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME, name = "grpc.server")
public class GrpcServerBuildTimeConfig {
    /**
     * Whether or not a health check on gRPC status is published in case the smallrye-health extension is present.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean mpHealthEnabled;

    /**
     * Whether or not the gRPC health check is exposed.
     */
    @ConfigItem(name = "grpc-health.enabled", defaultValue = "true")
    public boolean grpcHealthEnabled;
}
