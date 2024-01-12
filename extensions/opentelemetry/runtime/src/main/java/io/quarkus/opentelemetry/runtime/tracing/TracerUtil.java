package io.quarkus.opentelemetry.runtime.tracing;

import static io.opentelemetry.semconv.ResourceAttributes.HOST_NAME;
import static io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAME;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;

public class TracerUtil {

    private TracerUtil() {
    }

    public static Resource mapResourceAttributes(List<String> resourceAttributes, String serviceName, String hostname) {
        final AttributesBuilder attributesBuilder = Attributes.builder();

        if (!resourceAttributes.isEmpty()) {
            OpenTelemetryUtil
                    .convertKeyValueListToMap(resourceAttributes)
                    .forEach(attributesBuilder::put);
        }

        if (serviceName != null) {
            attributesBuilder.put(SERVICE_NAME.getKey(), serviceName);
        }

        if (hostname != null) {
            attributesBuilder.put(HOST_NAME, hostname);
        }

        return Resource.create(attributesBuilder.build());
    }
}
