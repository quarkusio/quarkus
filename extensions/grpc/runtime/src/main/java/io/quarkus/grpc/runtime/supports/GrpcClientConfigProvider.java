package io.quarkus.grpc.runtime.supports;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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

}
