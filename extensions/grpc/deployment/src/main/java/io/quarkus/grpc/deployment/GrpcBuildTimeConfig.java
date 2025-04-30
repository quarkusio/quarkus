package io.quarkus.grpc.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.grpc")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface GrpcBuildTimeConfig {

    /**
     * Configuration gRPC dev mode.
     */
    @ConfigDocSection(generated = true)
    GrpcDevModeConfig devMode();

    @ConfigGroup
    interface GrpcDevModeConfig {

        /**
         * Start gRPC server in dev mode even if no gRPC services are implemented.
         * By default set to `true` to ease incremental development of new services using dev mode.
         */
        @WithDefault("true")
        boolean forceServerStart();
    }
}
