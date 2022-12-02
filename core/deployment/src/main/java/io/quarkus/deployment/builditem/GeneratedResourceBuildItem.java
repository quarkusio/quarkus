package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

public final class GeneratedResourceBuildItem extends MultiBuildItem {
    final String name;
    final byte[] classData;

    // This option is only meant to be set by extensions that also generated the resource on the file system
    // and must rely on Quarkus not getting in the way of loading that resource.
    // It is currently used by Kogito to get serving of static resources in Dev Mode by Vert.x
    final boolean excludeFromDevCL;

    public GeneratedResourceBuildItem(String name, byte[] classData) {
        this.name = name;
        this.classData = classData;
        this.excludeFromDevCL = false;
    }

    public GeneratedResourceBuildItem(String name, byte[] classData, boolean excludeFromDevCL) {
        this.name = name;
        this.classData = classData;
        this.excludeFromDevCL = excludeFromDevCL;
    }

    public String getName() {
        return name;
    }

    public byte[] getClassData() {
        return classData;
    }

    public boolean isExcludeFromDevCL() {
        return excludeFromDevCL;
    }
}
