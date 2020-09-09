package io.quarkus.micrometer.runtime.binder.mpmetrics;

import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.Timed;

import io.micrometer.core.instrument.Timer;

@SuppressWarnings("unused")
@Timed
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 10)
class TimedInterceptor {

    // Micrometer meter registry
    final MetricRegistryAdapter mpRegistry;

    TimedInterceptor(MetricRegistryAdapter mpRegistry) {
        this.mpRegistry = mpRegistry;
    }

    @AroundConstruct
    Object timedConstructor(InvocationContext context) throws Exception {
        return time(context, context.getConstructor().getDeclaringClass().getSimpleName());
    }

    @AroundInvoke
    Object timedMethod(InvocationContext context) throws Exception {
        return time(context, context.getMethod().getName());
    }

    @AroundTimeout
    Object timedTimeout(InvocationContext context) throws Exception {
        return time(context, context.getMethod().getName());
    }

    Object time(InvocationContext context, String methodName) throws Exception {
        Timed annotation = MpMetricsRegistryProducer.getAnnotation(context, Timed.class);
        if (annotation != null) {
            MpMetadata metadata = new MpMetadata(annotation.name().replace("<method>", methodName),
                    annotation.description().replace("<method>", methodName),
                    annotation.unit(),
                    MetricType.TIMER);
            TimerAdapter impl = mpRegistry.interceptorTimer(metadata, annotation.tags());

            Timer.Sample sample = impl.start();
            try {
                return context.proceed();
            } finally {
                try {
                    impl.stop(sample);
                } catch (Exception e) {
                    // ignoring on purpose
                }
            }
        }
        return context.proceed();
    }
}
