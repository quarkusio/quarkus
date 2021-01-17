package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

public final class GeneratedResourceBuildItem extends MultiBuildItem {
    private final String name;
    private final byte[] classData;
    /**
     * This setting is only relevant for the fast-jar package and determines whether the generated file should be included in
     * the
     * quarkus-run jar (if set to {@code true}) or the generated-bytecode jar (if set to {@code false}).
     * This option is generally not needed except for services that need to be loadable by the (few) jars that are included
     * in the quarkus-run (that is the bootstrap) jar
     */
    private final boolean includeInBootstrap;

    public GeneratedResourceBuildItem(String name, byte[] classData) {
        this(name, classData, false);
    }

    public GeneratedResourceBuildItem(String name, byte[] classData, boolean includeInBootstrap) {
        this.name = name;
        this.classData = classData;
        this.includeInBootstrap = includeInBootstrap;
    }

    public String getName() {
        return name;
    }

    public byte[] getClassData() {
        return classData;
    }

    public boolean isIncludeInBootstrap() {
        return includeInBootstrap;
    }
}
