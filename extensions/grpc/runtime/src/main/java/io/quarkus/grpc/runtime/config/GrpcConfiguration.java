package io.quarkus.grpc.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;

/**
 * gRPC configuration root.
 */
@ConfigMapping(prefix = "quarkus.grpc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface GrpcConfiguration {

    /**
     * Configures the gRPC clients.
     */
    @WithDefaults
    @ConfigDocMapKey("client-name")
    @ConfigDocSection(generated = true)
    Map<String, GrpcClientConfiguration> clients();

    /**
     * Configure the gRPC server.
     */
    @ConfigDocSection(generated = true)
    GrpcServerConfiguration server();

}
