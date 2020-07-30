package io.quarkus.micrometer.runtime.binder.mpmetrics;

import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.Counted;

@SuppressWarnings("unused")
@Counted
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 10)
class CountedInterceptor {

    // Micrometer meter registry
    final MetricRegistryAdapter mpRegistry;

    CountedInterceptor(MetricRegistryAdapter mpRegistry) {
        this.mpRegistry = mpRegistry;
    }

    @AroundConstruct
    Object countedConstructor(InvocationContext context) throws Exception {
        return increment(context, context.getConstructor().getDeclaringClass().getSimpleName());
    }

    @AroundInvoke
    Object countedMethod(InvocationContext context) throws Exception {
        return increment(context, context.getMethod().getName());
    }

    @AroundTimeout
    Object countedTimeout(InvocationContext context) throws Exception {
        return increment(context, context.getMethod().getName());
    }

    Object increment(InvocationContext context, String methodName) throws Exception {
        Counted annotation = MpMetricsRegistryProducer.getAnnotation(context, Counted.class);
        if (annotation != null) {
            MpMetadata metadata = new MpMetadata(annotation.name().replace("<method>", methodName),
                    annotation.description().replace("<method>", methodName),
                    annotation.unit(),
                    MetricType.COUNTER);

            mpRegistry.interceptorCounter(metadata, annotation.tags()).inc();
        }
        return context.proceed();
    }
}
