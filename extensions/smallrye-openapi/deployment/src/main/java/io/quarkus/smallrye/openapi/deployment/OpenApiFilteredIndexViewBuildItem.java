package io.quarkus.smallrye.openapi.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;

/**
 * The filtered Jandex index of the OpenApi
 */
public final class OpenApiFilteredIndexViewBuildItem extends SimpleBuildItem {

    private final FilteredIndexView index;

    public OpenApiFilteredIndexViewBuildItem(FilteredIndexView index) {
        this.index = index;
    }

    public FilteredIndexView getIndex() {
        return index;
    }
}
