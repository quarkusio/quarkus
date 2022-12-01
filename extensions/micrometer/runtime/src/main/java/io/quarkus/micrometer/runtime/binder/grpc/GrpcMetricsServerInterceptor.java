package io.quarkus.micrometer.runtime.binder.grpc;

import javax.enterprise.inject.spi.Prioritized;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor.Priority;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;

@Singleton
@GlobalInterceptor
public class GrpcMetricsServerInterceptor extends MetricCollectingServerInterceptor implements Prioritized {

    @Inject
    public GrpcMetricsServerInterceptor(MeterRegistry registry) {
        super(registry);
    }

    @Override
    public int getPriority() {
        return Priority.PLATFORM_AFTER;
    }

}
