package io.quarkus.undertow.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A generated static resource that will be served by the web container
 *
 * This is automatically registered in native mode
 */
public final class GeneratedWebResourceBuildItem extends MultiBuildItem {
    final String name;
    final byte[] classData;

    public GeneratedWebResourceBuildItem(String name, byte[] classData) {
        this.name = name;
        this.classData = classData;
    }

    public String getName() {
        return name;
    }

    public byte[] getClassData() {
        return classData;
    }
}
