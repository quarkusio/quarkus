package io.quarkus.micrometer.runtime.binder.grpc;

import jakarta.enterprise.inject.spi.Prioritized;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor.Priority;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingClientInterceptor;
import io.quarkus.grpc.GlobalInterceptor;

@Singleton
@GlobalInterceptor
public class GrpcMetricsClientInterceptor extends MetricCollectingClientInterceptor implements Prioritized {

    @Inject
    public GrpcMetricsClientInterceptor(MeterRegistry registry) {
        super(registry);
    }

    @Override
    public int getPriority() {
        return Priority.PLATFORM_AFTER;
    }

}
