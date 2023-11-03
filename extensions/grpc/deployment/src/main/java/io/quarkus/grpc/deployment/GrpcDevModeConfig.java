package io.quarkus.grpc.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class GrpcDevModeConfig {

    /**
     * Start gRPC server in dev mode even if no gRPC services are implemented.
     * By default set to `true` to ease incremental development of new services using dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean forceServerStart;
}
