package io.quarkus.opentelemetry.deployment.tracing;

import java.util.Arrays;
import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DropNonApplicationUrisBuildItem extends SimpleBuildItem {
    private List<String> dropNames;

    public DropNonApplicationUrisBuildItem(final List<String> dropNames) {
        this.dropNames = dropNames;
    }

    public DropNonApplicationUrisBuildItem(final String... dropNames) {
        this(Arrays.asList(dropNames));
    }

    public List<String> getDropNames() {
        return dropNames;
    }
}
