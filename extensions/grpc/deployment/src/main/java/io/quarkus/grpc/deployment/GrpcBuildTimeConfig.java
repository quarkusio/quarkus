package io.quarkus.grpc.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class GrpcBuildTimeConfig {

    /**
     * Configuration gRPC dev mode.
     */
    @ConfigItem
    public GrpcDevModeConfig devMode;

}
