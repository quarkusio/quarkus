package io.quarkus.micrometer.runtime.binder.mpmetrics;

import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;

import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;

import io.quarkus.arc.ArcInvocationContext;

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
    Object cGaugeConstructor(ArcInvocationContext context) throws Exception {
        return cGauge(context, context.getConstructor().getDeclaringClass().getSimpleName());
    }

    @AroundInvoke
    Object cGaugeMethod(ArcInvocationContext context) throws Exception {
        return cGauge(context, context.getMethod().getName());
    }

    Object cGauge(ArcInvocationContext context, String methodName) throws Exception {
        ConcurrentGauge annotation = context.findIterceptorBinding(ConcurrentGauge.class);
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
