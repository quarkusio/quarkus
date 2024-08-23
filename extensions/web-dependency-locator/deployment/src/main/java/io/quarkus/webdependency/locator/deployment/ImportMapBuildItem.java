package io.quarkus.webdependency.locator.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ImportMapBuildItem extends SimpleBuildItem {
    private final String importmap;

    public ImportMapBuildItem(String importmap) {
        this.importmap = importmap;
    }

    public String getImportMap() {
        return this.importmap;
    }
}
