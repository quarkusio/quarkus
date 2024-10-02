package io.quarkus.grpc.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * gRPC configuration root.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class GrpcConfiguration {

    /**
     * Configures the gRPC clients.
     */
    @ConfigItem
    @ConfigDocSection(generated = true)
    @ConfigDocMapKey("client-name")
    public Map<String, GrpcClientConfiguration> clients;

    /**
     * Configure the gRPC server.
     */
    @ConfigDocSection(generated = true)
    public GrpcServerConfiguration server;

}
