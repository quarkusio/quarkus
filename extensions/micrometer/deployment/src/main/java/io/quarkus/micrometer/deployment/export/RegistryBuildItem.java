package io.quarkus.micrometer.deployment.export;

import io.quarkus.builder.item.MultiBuildItem;

public final class RegistryBuildItem extends MultiBuildItem {

    private final String name;

    private final String path;

    public RegistryBuildItem(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String name() {
        return name;
    }

    public String path() {
        return path;
    }
}
