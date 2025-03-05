package io.quarkus.grpc.api;

import java.util.Map;

import io.grpc.ManagedChannelBuilder;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.vertx.grpc.client.GrpcClientOptions;

/**
 * Allow for customization of Channel building.
 * Implement the customize method, depending on which Channel implementation you're going to use,
 * e.g. Vert.x or Netty.
 * This is an experimental API, subject to change.
 */
public interface ChannelBuilderCustomizer<T extends ManagedChannelBuilder<T>> {

    /**
     * Customize a ManagedChannelBuilder instance.
     *
     * @param name gRPC client name
     * @param config client's configuration
     * @param builder Channel builder instance
     * @return map of config properties to be used as default service config against the builder
     */
    default Map<String, Object> customize(String name, GrpcClientConfiguration config, T builder) {
        return Map.of();
    }

    /**
     * Customize a GrpcClientOptions instance.
     *
     * @param name gRPC client name
     * @param config client's configuration
     * @param options GrpcClientOptions instance
     */
    default void customize(String name, GrpcClientConfiguration config, GrpcClientOptions options) {
    }

    /**
     * Priority by which the customizers are applied.
     * Higher priority is applied later.
     *
     * @return the priority
     */
    default int priority() {
        return 0;
    }
}
