package io.quarkus.grpc.api;

import io.grpc.ServerBuilder;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.vertx.grpc.server.GrpcServerOptions;

/**
 * Allow for customization of Server building.
 * Implement the customize method, depending on which ServerBuilder implementation you're going to use,
 * e.g. Vert.x or Netty.
 * This is an experimental API, subject to change.
 */
public interface ServerBuilderCustomizer<T extends ServerBuilder<T>> {

    /**
     * Customize a ServerBuilder instance.
     *
     * @param config server's configuration
     * @param builder Server builder instance
     */
    default void customize(GrpcServerConfiguration config, T builder) {
    }

    /**
     * Customize a GrpcServerOptions instance.
     *
     * @param config server's configuration
     * @param options GrpcServerOptions instance
     */
    default void customize(GrpcServerConfiguration config, GrpcServerOptions options) {
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
