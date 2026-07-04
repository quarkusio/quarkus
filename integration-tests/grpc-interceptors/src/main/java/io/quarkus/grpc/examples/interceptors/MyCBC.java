package io.quarkus.grpc.examples.interceptors;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.grpc.api.ChannelBuilderCustomizer;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.vertx.grpc.client.GrpcClientOptions;

@SuppressWarnings("rawtypes")
@ApplicationScoped
public class MyCBC implements ChannelBuilderCustomizer {
    public static final AtomicBoolean USED = new AtomicBoolean(false);

    @Override
    public void customize(String name, GrpcClientConfiguration config, GrpcClientOptions options) {
        USED.set(true);
    }
}
