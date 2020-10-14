package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

public final class GeneratedClassBuildItem extends MultiBuildItem {

    final boolean applicationClass;
    final String name;
    final byte[] classData;
    final String source;

    public GeneratedClassBuildItem(boolean applicationClass, String name, byte[] classData) {
        this(applicationClass, name, classData, null);
    }

    public GeneratedClassBuildItem(boolean applicationClass, String name, byte[] classData, String source) {
        if (name.startsWith("/")) {
            throw new IllegalArgumentException("Name cannot start with '/':" + name);
        }
        this.applicationClass = applicationClass;
        this.name = name;
        this.classData = classData;
        this.source = source;
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

    public String getSource() {
        return source;
    }

}
