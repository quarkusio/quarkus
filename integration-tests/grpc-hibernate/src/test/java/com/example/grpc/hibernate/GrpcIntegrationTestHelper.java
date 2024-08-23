package com.example.grpc.hibernate;

import java.lang.reflect.Constructor;
import java.util.function.Function;

import io.grpc.stub.AbstractStub;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;

public class GrpcIntegrationTestHelper {

    private static Vertx vertx;
    private static GrpcClient grpcClient;

    static void init() {
        vertx = Vertx.vertx();
        grpcClient = GrpcClient.client(vertx);
    }

    static void cleanup() {
        grpcClient.close().toCompletionStage().toCompletableFuture().join();
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }

    static <T> T createClient(int port, Class<T> clazz, Function<GrpcClientChannel, AbstractStub<?>> function) {
        try {
            GrpcClientChannel channel = new GrpcClientChannel(grpcClient, SocketAddress.inetSocketAddress(port, "localhost"));
            var stub = function.apply(channel);
            Constructor<T> constructor = clazz.getDeclaredConstructor(stub.getClass());
            constructor.setAccessible(true);
            return constructor.newInstance(stub);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
