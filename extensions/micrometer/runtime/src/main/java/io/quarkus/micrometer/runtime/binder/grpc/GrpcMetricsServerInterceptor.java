package io.quarkus.micrometer.runtime.binder.grpc;

import java.util.function.UnaryOperator;

import jakarta.enterprise.inject.spi.Prioritized;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor.Priority;

import io.grpc.Status;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

@Singleton
@GlobalInterceptor
public class GrpcMetricsServerInterceptor extends MetricCollectingServerInterceptor implements Prioritized {

    @Inject
    public GrpcMetricsServerInterceptor(MeterRegistry registry, MicrometerConfig config) {
        super(registry, UnaryOperator.identity(),
                GrpcMetricTimerCustomizer.create(config.binder().grpcServer().histogram(),
                        config.binder().grpcServer().slos()),
                Status.Code.OK);
    }

    @Override
    public int getPriority() {
        return Priority.PLATFORM_AFTER;
    }

}
