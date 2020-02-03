package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A generated class that is only applicable to native images
 */
public final class GeneratedNativeImageClassBuildItem extends MultiBuildItem
        implements Comparable<GeneratedNativeImageClassBuildItem> {
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

    @Override
    public int compareTo(GeneratedNativeImageClassBuildItem other) {
        return this.name.compareTo(other.name);
    }
}
