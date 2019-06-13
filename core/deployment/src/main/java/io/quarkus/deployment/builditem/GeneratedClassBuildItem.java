package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

public final class GeneratedClassBuildItem extends MultiBuildItem {
    final boolean applicationClass;
    final String name;
    final byte[] classData;

    public GeneratedClassBuildItem(boolean applicationClass, String name, byte[] classData) {
        this.applicationClass = applicationClass;
        this.name = name;
        this.classData = classData;
    }

    public boolean isApplicationClass() {
        return applicationClass;
    }

    public String getName() {
        return name;
    }

    public byte[] getClassData() {
        return classData;
    }
}
