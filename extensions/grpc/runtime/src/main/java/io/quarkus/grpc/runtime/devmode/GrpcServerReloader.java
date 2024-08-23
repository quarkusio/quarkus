package io.quarkus.grpc.runtime.devmode;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.internal.ServerImpl;
import io.quarkus.dev.testing.GrpcWebSocketProxy;
import io.quarkus.grpc.stubs.ServerCalls;
import io.quarkus.grpc.stubs.StreamCollector;
import io.quarkus.runtime.LaunchMode;
import io.vertx.grpc.VertxServer;

public class GrpcServerReloader {
    private static volatile VertxServer server = null;

    public static VertxServer getServer() {
        return server;
    }

    public static void init(VertxServer grpcServer) {
        server = grpcServer;
        ServerCalls.setStreamCollector(GrpcServerReloader.devModeCollector());
    }

    public static StreamCollector devModeCollector() {
        if (LaunchMode.current() != LaunchMode.DEVELOPMENT) {
            throw new IllegalStateException("Attempted to initialize development mode StreamCollector in non-development mode");
        }
        return new DevModeStreamsCollector();
    }

    public static void reset() {
        try {
            if (server == null) {
                return;
            }

            Field registryField = ServerImpl.class.getDeclaredField("registry");
            registryField.setAccessible(true);
            Object registryObject = registryField.get(server.getRawServer());

            forceSet(registryObject, "services", null);
            forceSet(registryObject, "methods", null);
            forceSet(server.getRawServer(), "interceptors", null);

            StreamCollector streamCollector = ServerCalls.getStreamCollector();
            if (!(streamCollector instanceof DevModeStreamsCollector)) {
                throw new IllegalStateException("Non-dev mode streams collector used in development mode");
            }
            ((DevModeStreamsCollector) streamCollector).shutdown();
            GrpcWebSocketProxy.closeAll();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to reinitialize gRPC server", e);
        }
    }

    public static void reinitialize(List<ServerServiceDefinition> serviceDefinitions,
            Map<String, ServerMethodDefinition<?, ?>> methods,
            List<ServerInterceptor> sortedInterceptors) {
        if (server == null) {
            return;
        }
        try {

            Field registryField = ServerImpl.class.getDeclaredField("registry");
            registryField.setAccessible(true);
            Object registryObject = registryField.get(server.getRawServer());
            forceSet(registryObject, "services", serviceDefinitions);
            forceSet(registryObject, "methods", methods);

            ServerInterceptor[] interceptorsArray = sortedInterceptors.toArray(new ServerInterceptor[0]);
            forceSet(server.getRawServer(), "interceptors", interceptorsArray);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to reinitialize gRPC server data", e);
        }
    }

    private static void forceSet(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        field.set(object, value);
    }

    public static void shutdown() {
        if (server != null) {
            try {
                server.shutdown();
            } catch (RejectedExecutionException ignored) {
                // Ignore this, it means the application is already shutting down
            } finally {
                server = null;
            }

        }
    }
}
