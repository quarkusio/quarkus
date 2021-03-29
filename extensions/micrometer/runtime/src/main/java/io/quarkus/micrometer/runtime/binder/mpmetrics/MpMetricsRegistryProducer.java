package io.quarkus.micrometer.runtime.binder.mpmetrics;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.*;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.ArcInvocationContext;

@Singleton
@SuppressWarnings("unused")
class MpMetricsRegistryProducer {
    @Produces
    @Singleton
    public MetricRegistryAdapter produceRegistry(MeterRegistry registry) {
        return MpMetricsRecorder.getRegistry(MetricRegistry.Type.APPLICATION);
    }

    @Produces
    @Singleton
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    public MetricRegistryAdapter produceApplicationRegistry(MeterRegistry registry) {
        return MpMetricsRecorder.getRegistry(MetricRegistry.Type.APPLICATION);
    }

    @Produces
    @Singleton
    @RegistryType(type = MetricRegistry.Type.BASE)
    public MetricRegistry produceBaseRegistry(MeterRegistry registry) {
        return MpMetricsRecorder.getRegistry(MetricRegistry.Type.BASE);
    }

    @Produces
    @Singleton
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    public MetricRegistry produceVendorRegistry(MeterRegistry registry) {
        return MpMetricsRecorder.getRegistry(MetricRegistry.Type.VENDOR);
    }

    public static <T> T getAnnotation(InvocationContext context, Class<T> annotationClass) {
        Set<Annotation> annotations = (Set<Annotation>) context.getContextData()
                .get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);

        for (Annotation a : annotations) {
            if (annotationClass.isInstance(a)) {
                return annotationClass.cast(a);
            }
        }
        return null;
    }
}
