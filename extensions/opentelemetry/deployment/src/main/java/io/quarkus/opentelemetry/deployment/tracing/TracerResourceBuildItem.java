package io.quarkus.opentelemetry.deployment.tracing;

import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class TracerResourceBuildItem extends SimpleBuildItem {
    private final RuntimeValue<Resource> resource;

    public TracerResourceBuildItem(RuntimeValue<Resource> resource) {
        this.resource = resource;
    }

    public RuntimeValue<Resource> getResource() {
        return resource;
    }
}
