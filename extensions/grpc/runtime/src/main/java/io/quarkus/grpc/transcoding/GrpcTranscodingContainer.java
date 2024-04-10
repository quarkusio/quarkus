package io.quarkus.grpc.transcoding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.grpc.GrpcTranscoding;

@ApplicationScoped
public class GrpcTranscodingContainer {

    @Inject
    Instance<GrpcTranscoding> services;

    public Instance<GrpcTranscoding> getServices() {
        return services;
    }
}
