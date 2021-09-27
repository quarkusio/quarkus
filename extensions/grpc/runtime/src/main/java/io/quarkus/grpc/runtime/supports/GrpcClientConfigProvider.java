package io.quarkus.grpc.runtime.supports;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.grpc.stub.AbstractStub;
import io.quarkus.arc.Arc;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;

/**
 * This bean provides the {@link GrpcClientConfiguration} to {@link Channels}.
 */
@ApplicationScoped
public class GrpcClientConfigProvider {

    @Inject
    GrpcConfiguration config;

    public GrpcClientConfiguration getConfiguration(String name) {
        Map<String, GrpcClientConfiguration> clients = config.clients;
        if (clients == null) {
            return null;
        } else {
            return clients.get(name);
        }
    }

    public GrpcServerConfiguration getServerConfiguration() {
        return config.server;
    }

    AbstractStub<?> adjustCallOptions(String serviceName, AbstractStub<?> stub) {
        GrpcClientConfiguration clientConfig = config.clients != null ? config.clients.get(serviceName) : null;
        if (clientConfig != null) {
            if (clientConfig.compression.isPresent()) {
                stub = stub.withCompression(clientConfig.compression.get());
            }
            if (clientConfig.deadline.isPresent()) {
                Duration deadline = clientConfig.deadline.get();
                stub = stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
            }
        }
        return stub;
    }

    public static AbstractStub<?> configureStub(String serviceName, AbstractStub<?> stub) {
        return Arc.container().instance(GrpcClientConfigProvider.class).get().adjustCallOptions(serviceName, stub);
    }

    public static AbstractStub<?> addBlockingClientInterceptor(AbstractStub<?> stub) {
        return stub.withInterceptors(new EventLoopBlockingCheckInterceptor());
    }

    public static BiFunction<String, AbstractStub<?>, AbstractStub<?>> getStubConfigurator() {
        return GrpcClientConfigProvider::configureStub;
    }

}
