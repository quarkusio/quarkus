package io.quarkus.smallrye.metrics.runtime;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Supplier;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import io.smallrye.metrics.MetricRegistries;

final class FilterUtil {

    private FilterUtil() {
    }

    static void finishRequest(Long start, Class<?> resourceClass, String methodName, Class<?>[] parameterTypes,
            Supplier<Boolean> wasSuccessful) {
        long value = System.nanoTime() - start;
        boolean success = wasSuccessful.get();
        MetricID metricID = getMetricID(resourceClass, methodName, parameterTypes, success);

        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
        if (success) {
            registry.simpleTimer(metricID).update(Duration.ofNanos(value));
        } else {
            registry.counter(metricID).inc();
        }
    }

    static void maybeCreateMetrics(Class<?> resourceClass, Method resourceMethod) {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
        MetricID success = getMetricID(resourceClass, resourceMethod.getName(), resourceMethod.getParameterTypes(), true);
        if (registry.getSimpleTimer(success) == null) {
            Metadata successMetadata = Metadata.builder()
                    .withName(success.getName())
                    .withDescription(
                            "The number of invocations and total response time of this RESTful " +
                                    "resource method since the start of the server.")
                    .withUnit(MetricUnits.NANOSECONDS)
                    .build();
            registry.simpleTimer(successMetadata, success.getTagsAsArray());
        }
        MetricID failure = getMetricID(resourceClass, resourceMethod.getName(), resourceMethod.getParameterTypes(), false);
        if (registry.getCounter(failure) == null) {
            Metadata failureMetadata = Metadata.builder()
                    .withName(failure.getName())
                    .withDisplayName("Total Unmapped Exceptions count")
                    .withDescription(
                            "The total number of unmapped exceptions that occurred from this RESTful resource " +
                                    "method since the start of the server.")
                    .build();
            registry.counter(failureMetadata, failure.getTagsAsArray());
        }
    }

    static MetricID getMetricID(Class<?> resourceClass, String methodName, Class<?>[] parameterTypes,
            boolean requestWasSuccessful) {
        Tag classTag = new Tag("class", resourceClass.getName());
        StringBuilder sb = new StringBuilder();
        for (Class<?> parameterType : parameterTypes) {
            if (sb.length() > 0) {
                sb.append("_");
            }
            if (parameterType.isArray()) {
                sb.append(parameterType.getComponentType().getName()).append("[]");
            } else {
                sb.append(parameterType.getName());
            }
        }
        String encodedParameterNames = sb.toString();
        String methodTagValue = encodedParameterNames.isEmpty() ? methodName : methodName + "_" + encodedParameterNames;
        Tag methodTag = new Tag("method", methodTagValue);
        String name = requestWasSuccessful ? "REST.request" : "REST.request.unmappedException.total";
        return new MetricID(name, classTag, methodTag);
    }
}
