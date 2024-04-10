package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration root for gRPC Transcoding feature in Quarkus. gRPC Transcoding allows you to create
 * RESTful JSON APIs that are backed by existing gRPC services.
 */
@ConfigRoot(name = "grpc.transcoding", phase = ConfigPhase.BUILD_TIME)
public class GrpcTranscodingConfig {

    /**
     * Flag to enable or disable the gRPC Transcoding feature.
     * The default value is `false` (disabled).
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;
}
