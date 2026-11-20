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
     * {@code Resource} CDI beans. The attributes the SDK builds resource entities from are left out:
     * the SDK already applied them, and writing them again here would drop the entity association.
     */
    public static Resource mapResourceAttributes(List<String> resourceAttributes, String hostname) {
        final AttributesBuilder attributesBuilder = Attributes.builder();

        if (!resourceAttributes.isEmpty()) {
            for (Map.Entry<String, String> attribute : OpenTelemetryUtil
                    .convertKeyValueListToMap(resourceAttributes).entrySet()) {
                if (!ResourceConfigReconciler.ENTITY_OWNED_ATTRIBUTES.contains(attribute.getKey())) {
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
