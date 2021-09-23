package io.quarkus.grpc.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class GrpcBuildTimeConfig {
    /**
     * Whether or not metrics are published in case a metrics extension is present.
     */
    @ConfigItem(name = "metrics.enabled")
    public boolean metricsEnabled;

    /**
     * Configuration gRPC dev mode.
     */
    @ConfigItem
    public GrpcDevModeConfig devMode;

}
