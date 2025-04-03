package io.quarkus.grpc.examples.interceptors;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.grpc.api.ServerBuilderCustomizer;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.vertx.grpc.VertxServerBuilder;
import io.vertx.grpc.server.GrpcServerOptions;

@ApplicationScoped
public class MySBC implements ServerBuilderCustomizer<VertxServerBuilder> {
    public static final AtomicBoolean USED = new AtomicBoolean(false);

    @Override
    public void customize(GrpcServerConfiguration config, VertxServerBuilder builder) {
        USED.set(true);
    }

    @Override
    public void customize(GrpcServerConfiguration config, GrpcServerOptions options) {
        USED.set(true);
    }
}
