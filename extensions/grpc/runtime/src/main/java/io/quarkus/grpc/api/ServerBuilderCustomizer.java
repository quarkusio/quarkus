package io.quarkus.grpc.api;

import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.vertx.grpc.server.GrpcServerOptions;

/**
 * Allow for customization of gRPC server options.
 * This is an experimental API, subject to change.
 */
public interface ServerBuilderCustomizer {

    /**
     * Customize a GrpcServerOptions instance.
     *
     * @param config server's configuration
     * @param options GrpcServerOptions instance
     */
    void customize(GrpcServerConfiguration config, GrpcServerOptions options);

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
