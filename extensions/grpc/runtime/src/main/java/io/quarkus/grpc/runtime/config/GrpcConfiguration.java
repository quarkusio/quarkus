package io.quarkus.grpc.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.*;

/**
 * gRPC configuration root.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class GrpcConfiguration {

    /**
     * Configures the gRPC clients.
     */
    @ConfigItem
    @ConfigDocSection
    @ConfigDocMapKey("client-name")
    public Map<String, GrpcClientConfiguration> clients;

    /**
     * Configure the gRPC server.
     */
    @ConfigDocSection
    public GrpcServerConfiguration server;

}
