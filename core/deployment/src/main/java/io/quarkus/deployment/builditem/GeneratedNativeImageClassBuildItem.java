package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A generated class that is only applicable to native images
 */
public final class GeneratedNativeImageClassBuildItem extends MultiBuildItem {
    final String name;
    final byte[] classData;

    public GeneratedNativeImageClassBuildItem(String name, byte[] classData) {
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
