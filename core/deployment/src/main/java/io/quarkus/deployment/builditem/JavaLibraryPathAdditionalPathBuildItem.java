package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item representing an additional path to be added to the Java library path (`java.library.path`).
 */
public final class JavaLibraryPathAdditionalPathBuildItem extends MultiBuildItem {

    private final String path;

    public JavaLibraryPathAdditionalPathBuildItem(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
