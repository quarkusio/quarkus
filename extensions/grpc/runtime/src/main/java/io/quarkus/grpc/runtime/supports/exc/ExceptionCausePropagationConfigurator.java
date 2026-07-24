package io.quarkus.grpc.runtime.supports.exc;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.grpc.ExceptionCauseSupport;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;

@ApplicationScoped
public class ExceptionCausePropagationConfigurator {

    @Inject
    GrpcConfiguration grpcConfiguration;

    @PostConstruct
    void init() {
        ExceptionCauseSupport.setPropagateExceptionCauses(grpcConfiguration.propagateExceptionCauses());
    }
}
