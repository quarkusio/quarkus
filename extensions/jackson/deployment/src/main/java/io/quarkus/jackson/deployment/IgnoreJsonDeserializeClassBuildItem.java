package io.quarkus.jackson.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used when an extension needs to inform the Jackson extension that a class should not
 * be registered for reflection even if it annotated with @JsonDeserialize
 */
public final class IgnoreJsonDeserializeClassBuildItem extends MultiBuildItem {

    private final DotName dotName;

    public IgnoreJsonDeserializeClassBuildItem(DotName dotName) {
        this.dotName = dotName;
    }

    public DotName getDotName() {
        return dotName;
    }
}
