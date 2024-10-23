package io.quarkus.opentelemetry.deployment.tracing;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents an application uri that must be ignored for tracing.
 */
public final class DropApplicationUrisBuildItem extends MultiBuildItem {

    private final String uri;

    public DropApplicationUrisBuildItem(String uri) {
        this.uri = uri;
    }

    public String uri() {
        return uri;
    }
}
