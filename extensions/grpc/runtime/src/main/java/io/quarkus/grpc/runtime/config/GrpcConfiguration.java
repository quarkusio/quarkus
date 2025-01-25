package io.quarkus.grpc.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * gRPC configuration root.
 */
@ConfigMapping(prefix = "quarkus.grpc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface GrpcConfiguration {

    /**
     * Configures the gRPC clients.
     */
    @ConfigDocSection(generated = true)
    @ConfigDocMapKey("client-name")
    Map<String, GrpcClientConfiguration> clients();

    /**
     * Configure the gRPC server.
     */
    @ConfigDocSection(generated = true)
    GrpcServerConfiguration server();

}
