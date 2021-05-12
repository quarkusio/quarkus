package io.quarkus.grpc.runtime.supports;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.grpc.stub.AbstractStub;
import io.quarkus.arc.Arc;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;

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

    AbstractStub<?> adjustCallOptions(String serviceName, AbstractStub<?> stub) {
        GrpcClientConfiguration clientConfig = config.clients != null ? config.clients.get(serviceName) : null;
        if (clientConfig != null) {
            if (clientConfig.compression.isPresent()) {
                stub = stub.withCompression(clientConfig.compression.get());
            }
        }
        return stub;
    }

    public static AbstractStub<?> configureStub(String serviceName, AbstractStub<?> stub) {
        return Arc.container().instance(GrpcClientConfigProvider.class).get().adjustCallOptions(serviceName, stub);
    }

}
