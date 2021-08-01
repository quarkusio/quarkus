package io.quarkus.micrometer.runtime.binder.mpmetrics;

import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;

import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.Counted;

import io.quarkus.arc.ArcInvocationContext;

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
    Object countedConstructor(ArcInvocationContext context) throws Exception {
        return increment(context, context.getConstructor().getDeclaringClass().getSimpleName());
    }

    @AroundInvoke
    Object countedMethod(ArcInvocationContext context) throws Exception {
        return increment(context, context.getMethod().getName());
    }

    Object increment(ArcInvocationContext context, String methodName) throws Exception {
        Counted annotation = context.findIterceptorBinding(Counted.class);
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
