package io.quarkus.deployment.builditem;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalClassLoaderResourcesBuildItem extends MultiBuildItem {

    final Map<String, byte[]> resources;

    public AdditionalClassLoaderResourcesBuildItem(Map<String, byte[]> resources) {
        this.resources = resources;
    }

    public Map<String, byte[]> getResources() {
        return resources;
    }

}
