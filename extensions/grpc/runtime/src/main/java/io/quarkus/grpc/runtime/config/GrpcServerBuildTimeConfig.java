package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.grpc.server")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface GrpcServerBuildTimeConfig {
    /**
     * Whether the gRPC server is enabled.
     * <p>
     * When disabled, no gRPC server is started and no gRPC server-related beans or health checks are registered, even
     * if the application contains {@code @GrpcService} beans. This is useful for applications that only use gRPC
     * clients.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether a health check on gRPC status is published in case the smallrye-health extension is present.
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean mpHealthEnabled();

    /**
     * Whether the gRPC health check is exposed.
     */
    @WithName("grpc-health.enabled")
    @WithDefault("true")
    boolean grpcHealthEnabled();
}
