package io.quarkus.opentelemetry.runtime.tracing;

import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_NAME;

import java.util.List;
import java.util.Map;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;
import io.quarkus.opentelemetry.runtime.ResourceConfigReconciler;

public class TracerUtil {

    private TracerUtil() {
    }

    /**
     * The attributes Quarkus applies last, so that they win over the defaults and over the
     * {@code Resource} CDI beans.
     * <p>
     * Only {@code service.name} is left out. The SDK always builds the {@code service} entity from
     * {@code otel.service.name}, so writing the attribute again here would drop that association.
     * {@code service.instance.id} is deliberately kept: when the user supplies one,
     * {@link ResourceConfigReconciler} switches the SDK's provider off, so there is no
     * {@code service.instance} entity left to collide with — and writing it here is the only thing
     * that delivers the value when the attributes were given in the indexed list form, which the SDK
     * never sees.
     */
    public static Resource mapResourceAttributes(List<String> resourceAttributes, String hostname) {
        final AttributesBuilder attributesBuilder = Attributes.builder();

        if (!resourceAttributes.isEmpty()) {
            for (Map.Entry<String, String> attribute : OpenTelemetryUtil
                    .convertKeyValueListToMap(resourceAttributes).entrySet()) {
                if (!ResourceConfigReconciler.SERVICE_NAME.equals(attribute.getKey())) {
                    attributesBuilder.put(attribute.getKey(), attribute.getValue());
                }
            }
        }

        if (hostname != null) {
            attributesBuilder.put(HOST_NAME, hostname);
        }

        return Resource.create(attributesBuilder.build());
    }
}
