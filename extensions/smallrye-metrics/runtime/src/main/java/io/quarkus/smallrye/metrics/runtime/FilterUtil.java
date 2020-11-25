package io.quarkus.smallrye.metrics.runtime;

import java.time.Duration;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import io.smallrye.metrics.MetricRegistries;

final class FilterUtil {

    private FilterUtil() {
    }

    static void finishRequest(Long start, Class<?> resourceClass, String methodName, Class<?>[] parameterTypes) {
        long value = System.nanoTime() - start;
        MetricID metricID = getMetricID(resourceClass, methodName, parameterTypes);

        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
        if (!registry.getMetadata().containsKey(metricID.getName())) {
            // if no metric with this name exists yet, register it
            Metadata metadata = Metadata.builder()
                    .withName(metricID.getName())
                    .withDescription(
                            "The number of invocations and total response time of this RESTful resource method since the start of the server.")
                    .withUnit(MetricUnits.NANOSECONDS)
                    .build();
            registry.simpleTimer(metadata, metricID.getTagsAsArray());
        }
        registry.simpleTimer(metricID.getName(), metricID.getTagsAsArray())
                .update(Duration.ofNanos(value));
    }

    private static MetricID getMetricID(Class<?> resourceClass, String methodName, Class<?>[] parameterTypes) {
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
        return new MetricID("REST.request", classTag, methodTag);
    }
}
