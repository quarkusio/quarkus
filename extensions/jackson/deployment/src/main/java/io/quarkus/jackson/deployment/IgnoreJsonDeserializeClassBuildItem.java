package io.quarkus.jackson.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used when an extension needs to inform the Jackson extension that a class should not be registered for reflection
 * even if it annotated with @JsonDeserialize
 */
public final class IgnoreJsonDeserializeClassBuildItem extends MultiBuildItem {

    private final List<DotName> dotNames;

    public IgnoreJsonDeserializeClassBuildItem(DotName dotName) {
        this.dotNames = List.of(dotName);
    }

    public IgnoreJsonDeserializeClassBuildItem(List<DotName> dotNames) {
        this.dotNames = dotNames;
    }

    @Deprecated(forRemoval = true)
    public DotName getDotName() {
        return dotNames.size() > 0 ? dotNames.get(0) : null;
    }

    public List<DotName> getDotNames() {
        return dotNames;
    }
}
