package io.quarkus.grpc.xds.devmode;

import java.util.List;
import java.util.Map;

import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;

// TODO
public class XdsServerReloader {
    private static volatile Server server;

    public static Server getServer() {
        return server;
    }

    public static void init(Server grpcServer) {
        server = grpcServer;
    }

    public static void reset() {
        shutdown();
    }

    public static void reinitialize(List<ServerServiceDefinition> serviceDefinitions,
            Map<String, ServerMethodDefinition<?, ?>> methods, List<ServerInterceptor> sortedInterceptors) {
        server = null;
    }

    public static void shutdown() {
        shutdown(server);
        server = null;
    }

    public static void shutdown(Server current) {
        if (current != null) {
            current.shutdownNow();
        }
    }
}
