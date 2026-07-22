package io.quarkus.micrometer.runtime.binder.grpc;

import java.util.function.UnaryOperator;

import jakarta.enterprise.inject.spi.Prioritized;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor.Priority;

import io.grpc.Status;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.grpc.MetricCollectingClientInterceptor;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

@Singleton
@GlobalInterceptor
public class GrpcMetricsClientInterceptor extends MetricCollectingClientInterceptor implements Prioritized {

    @Inject
    public GrpcMetricsClientInterceptor(MeterRegistry registry, MicrometerConfig config) {
        super(registry, UnaryOperator.identity(),
                GrpcMetricTimerCustomizer.create(config.binder().grpcClient().histogram(),
                        config.binder().grpcClient().slos()),
                Status.Code.OK);
    }

    @Override
    public int getPriority() {
        return Priority.PLATFORM_AFTER;
    }

}
