package io.quarkus.grpc.runtime;

import io.quarkus.registry.ValueRegistry;
import io.quarkus.registry.ValueRegistry.RuntimeInfo;
import io.quarkus.registry.ValueRegistry.RuntimeKey;
import io.quarkus.vertx.http.HttpServer;

/**
 * Represent the actual runtime values of the Quarkus gRPC Server.
 */
public interface GrpcServer {
    RuntimeKey<Integer> GRPC_PORT = RuntimeKey.intKey("quarkus.grpc.server.port");
    RuntimeKey<Integer> GRPC_TEST_PORT = RuntimeKey.intKey("quarkus.grpc.server.test-port");
    RuntimeKey<Boolean> GRPC_SEPARATE_SERVER = RuntimeKey.booleanKey("quarkus.grpc.server.use-separate-server");

    RuntimeKey<GrpcServer> GRPC_SERVER = RuntimeKey.key(GrpcServer.class);

    /**
     * Return the gRPC port that Quarkus is listening on.
     *
     * @return the port or <code>-1</code> if Quarkus is not set to listen to the gRPC port.
     */
    int getPort();

    /**
     * Checks if the gRPC is running in a separate server or running in the Vertx Http Server.
     *
     * @return <code>true</code> if the gRPC is running in a separate server or <code>false</code> otherwise.
     */
    boolean isSeparateServer();

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
                    if (!isSeparateServer()) {
                        return httpServer.getSecurePort() != -1 ? httpServer.getSecurePort() : httpServer.getPort();
                    } else {
                        return valueRegistry.getOrDefault(GRPC_PORT, -1);
                    }
                }

                @Override
                public boolean isSeparateServer() {
                    return valueRegistry.getOrDefault(GRPC_SEPARATE_SERVER, true);
                }
            };
        }
    };
}
