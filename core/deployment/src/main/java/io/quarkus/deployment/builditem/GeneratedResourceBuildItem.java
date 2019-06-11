package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

public final class GeneratedResourceBuildItem extends MultiBuildItem {
    final String name;
    final byte[] classData;

    public GeneratedResourceBuildItem(String name, byte[] classData) {
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
