package io.quarkus.micrometer.runtime.binder.mpmetrics;

import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;

@ConcurrentGauge
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 10)
class ConcurrentGaugeInterceptor {

    // Micrometer meter registry
    final MetricRegistryAdapter mpRegistry;

    ConcurrentGaugeInterceptor(MetricRegistryAdapter mpRegistry) {
        this.mpRegistry = mpRegistry;
    }

    @AroundConstruct
    Object cGaugeConstructor(InvocationContext context) throws Exception {
        return cGauge(context, context.getConstructor().getDeclaringClass().getSimpleName());
    }

    @AroundInvoke
    Object cGaugeMethod(InvocationContext context) throws Exception {
        return cGauge(context, context.getMethod().getName());
    }

    @AroundTimeout
    Object cGaugeTimeout(InvocationContext context) throws Exception {
        return cGauge(context, context.getMethod().getName());
    }

    Object cGauge(InvocationContext context, String methodName) throws Exception {
        ConcurrentGauge annotation = MpMetricsRegistryProducer.getAnnotation(context, ConcurrentGauge.class);
        if (annotation != null) {
            MpMetadata metadata = new MpMetadata(annotation.name().replace("<method>", methodName),
                    annotation.description().replace("<method>", methodName),
                    annotation.unit(),
                    MetricType.CONCURRENT_GAUGE);

            ConcurrentGaugeImpl impl = mpRegistry.interceptorConcurrentGauge(metadata, annotation.tags());
            try {
                impl.inc();
                return context.proceed();
            } finally {
                impl.dec();
            }
        }
        return context.proceed();
    }
}
