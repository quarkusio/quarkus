package io.quarkus.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;

public final class JavaLibraryPathAdditionalPathBuildItem extends MultiBuildItem {

    private final String path;

    public JavaLibraryPathAdditionalPathBuildItem(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
