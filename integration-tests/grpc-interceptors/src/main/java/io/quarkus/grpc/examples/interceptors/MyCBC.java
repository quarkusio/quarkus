package io.quarkus.grpc.examples.interceptors;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;

import io.grpc.netty.NettyChannelBuilder;
import io.quarkus.grpc.api.ChannelBuilderCustomizer;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.vertx.grpc.client.GrpcClientOptions;

@ApplicationScoped
public class MyCBC implements ChannelBuilderCustomizer<NettyChannelBuilder> {
    public static final AtomicBoolean USED = new AtomicBoolean(false);

    @Override
    public Map<String, Object> customize(String name, GrpcClientConfiguration config, NettyChannelBuilder builder) {
        USED.set(true);
        return Map.of();
    }

    @Override
    public void customize(String name, GrpcClientConfiguration config, GrpcClientOptions options) {
        USED.set(true);
    }
}
