package io.quarkus.opentelemetry.runtime.tracing;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;

public class TracerUtil {

    private TracerUtil() {
    }

    public static Resource mapResourceAttributes(List<String> resourceAttributes, String serviceName) {
        if (resourceAttributes.isEmpty()) {
            return Resource.empty();
        }
        AttributesBuilder attributesBuilder = Attributes.builder();
        var attrNameToValue = OpenTelemetryUtil.convertKeyValueListToMap(resourceAttributes);

        // override both default (app name) and explicitly set resource attribute
        // it needs to be done manually because OpenTelemetry correctly sets 'otel.service.name'
        // to existing (incoming) resource, but customizer output replaces originally set service name
        if (serviceName != null) {
            attrNameToValue.put(SERVICE_NAME.getKey(), serviceName);
        }

        attrNameToValue.forEach(attributesBuilder::put);
        return Resource.create(attributesBuilder.build());
    }
}
