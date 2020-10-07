package io.quarkus.rest.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class ContainerResponseFilterBuildItem extends MultiBuildItem {

    private final String className;
    private final Integer priority;

    public ContainerResponseFilterBuildItem(String className) {
        this(className, null);
    }

    public ContainerResponseFilterBuildItem(String className, Integer priority) {
        this.className = className;
        this.priority = priority;
    }

    public String getClassName() {
        return className;
    }

    public Integer getPriority() {
        return priority;
    }
}
