package io.quarkus.it.faulttolerance;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.FaultTolerance;

@ApplicationScoped
public class PreconfiguredFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final FaultTolerance<String> FT = FaultTolerance.<String> create()
            .withRetry().maxRetries(10).done()
            .build();
}
