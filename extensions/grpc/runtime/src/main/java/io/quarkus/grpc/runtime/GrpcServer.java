package io.quarkus.grpc.runtime;

import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeInfo;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;
import io.quarkus.vertx.http.HttpServer;

/**
 * Represent the actual runtime values of the Quarkus gRPC Server.
 */
public interface GrpcServer {
    RuntimeKey<GrpcServer> GRPC_SERVER = RuntimeKey.key(GrpcServer.class);

    /**
     * Return the gRPC port that Quarkus is listening on.
     * gRPC always runs on the Vert.x HTTP server.
     *
     * @return the port or <code>-1</code> if Quarkus is not set to listen.
     */
    int getPort();

    /**
     * The {@link RuntimeInfo} implementation for {@link GrpcServer}. Construct instances of {@link GrpcServer} with
     * {@link ValueRegistry} values.
     */
    RuntimeInfo<GrpcServer> INFO = new RuntimeInfo<>() {
        @Override
        public GrpcServer get(ValueRegistry valueRegistry) {
            HttpServer httpServer = valueRegistry.get(HttpServer.HTTP_SERVER);
            return new GrpcServer() {
                @Override
                public int getPort() {
                    return httpServer.getSecurePort() != -1 ? httpServer.getSecurePort() : httpServer.getPort();
                }
            };
        }
    };
}
