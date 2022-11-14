package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;

public class TracerUtil {
    private TracerUtil() {
    }

    public static Resource mapResourceAttributes(List<String> resourceAttributes) {
        if (resourceAttributes.isEmpty()) {
            return Resource.empty();
        }
        AttributesBuilder attributesBuilder = Attributes.builder();
        OpenTelemetryUtil.convertKeyValueListToMap(resourceAttributes).forEach(attributesBuilder::put);
        return Resource.create(attributesBuilder.build());
    }
}
