package com.example.grpc.hibernate;

import java.lang.reflect.Constructor;
import java.util.function.Function;

import io.grpc.stub.AbstractStub;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpcio.client.GrpcIoClient;
import io.vertx.grpcio.client.GrpcIoClientChannel;

public class GrpcIntegrationTestHelper {

    private static Vertx vertx;
    private static GrpcIoClient grpcClient;

    static void init() {
        vertx = Vertx.vertx();
        grpcClient = GrpcIoClient.client(vertx);
    }

    static void cleanup() {
        grpcClient.close().toCompletionStage().toCompletableFuture().join();
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }

    static <T> T createClient(int port, Class<T> clazz, Function<GrpcIoClientChannel, AbstractStub<?>> function) {
        try {
            GrpcIoClientChannel channel = new GrpcIoClientChannel(grpcClient,
                    SocketAddress.inetSocketAddress(port, "localhost"));
            var stub = function.apply(channel);
            Constructor<T> constructor = clazz.getDeclaredConstructor(stub.getClass());
            constructor.setAccessible(true);
            return constructor.newInstance(stub);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
