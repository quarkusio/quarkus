package io.quarkus.grpc.test.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;

public class GRPCTestUtils {
    private static final Logger log = LoggerFactory.getLogger(GRPCTestUtils.class);

    public static Channel channel(Vertx vertx) {
        int port = vertx != null ? 8081 : 9001;
        return channel(vertx, port);
    }

    public static Channel channel(Vertx vertx, int port) {
        Channel channel;
        if (vertx != null) {
            GrpcClient client = GrpcClient.client(vertx);
            GrpcClientChannel gcc = new GrpcClientChannel(client, SocketAddress.inetSocketAddress(port, "localhost"));
            channel = new InternalChannel(gcc, client);
        } else {
            channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        }
        log.info("Channel: {}, port: {}", channel, port);
        return channel;
    }

    public static void close(Channel channel) {
        if (channel instanceof ManagedChannel) {
            ManagedChannel mc = (ManagedChannel) channel;
            mc.shutdownNow();
        } else if (channel instanceof InternalChannel) {
            InternalChannel ic = (InternalChannel) channel;
            ic.close();
        }
    }

    public static void close(Vertx vertx) {
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }

    public static void close(GrpcClient client) {
        client.close().toCompletionStage().toCompletableFuture().join();
    }

    public static InputStream stream(String resource) {
        return GRPCTestUtils.class.getClassLoader().getResourceAsStream(resource);
    }

    public static Map.Entry<GrpcClient, Channel> tls(Vertx vertx, String caPem, String clientPem, String clientKey)
            throws IOException {
        HttpClientOptions options = new HttpClientOptions();
        options.setUseAlpn(true);
        options.setSsl(true);
        Buffer buffer;
        try (InputStream stream = stream(caPem)) {
            buffer = Buffer.buffer(stream.readAllBytes());
        }
        Buffer cb;
        try (InputStream stream = stream(clientPem)) {
            cb = Buffer.buffer(stream.readAllBytes());
        }
        Buffer ck;
        try (InputStream stream = stream(clientKey)) {
            ck = Buffer.buffer(stream.readAllBytes());
        }
        options.setTrustOptions(new PemTrustOptions().addCertValue(buffer));
        options.setKeyCertOptions(new PemKeyCertOptions().setCertValue(cb).setKeyValue(ck));

        GrpcClient client = GrpcClient.client(vertx, options);
        Channel channel = new GrpcClientChannel(client, SocketAddress.inetSocketAddress(8444, "localhost"));

        return Map.entry(client, channel);
    }

    private static class InternalChannel extends Channel {
        private final Channel delegate;
        private final GrpcClient client;

        InternalChannel(Channel delegate, GrpcClient client) {
            this.delegate = delegate;
            this.client = client;
        }

        @Override
        public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
                MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
            return delegate.newCall(methodDescriptor, callOptions);
        }

        @Override
        public String authority() {
            return delegate.authority();
        }

        void close() {
            GRPCTestUtils.close(client);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
