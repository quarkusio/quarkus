package io.quarkus.opentelemetry.deployment.tracing;

import java.util.Optional;

import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class TracerResourceBuildItem extends SimpleBuildItem {
    private final RuntimeValue<Optional<Resource>> resource;

    public TracerResourceBuildItem(RuntimeValue<Optional<Resource>> resource) {
        this.resource = resource;
    }

    public RuntimeValue<Optional<Resource>> getResource() {
        return resource;
    }
}
