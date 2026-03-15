package org.acme;

import jakarta.inject.Inject;

import io.grpc.Channel;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;

class GrpcCompressionInterceptorIntegrationCdiTestBase extends GrpcCompressionInterceptorIntegrationTestBase {

    @GrpcClient("hello-service")
    Channel channel;

    @Inject
    GrpcConfiguration configuration;

    @Override
    protected Channel getChannel() {
        return channel;
    }

    @Override
    protected int getPort() {
        return configuration.server().useSeparateServer() ? 9001 : 8081;
    }
}
