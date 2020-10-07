package io.quarkus.rest.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class ContainerRequestFilterBuildItem extends MultiBuildItem {

    private final String className;
    private final Integer priority;
    private final Boolean preMatching;

    public ContainerRequestFilterBuildItem(String className) {
        this(className, null, null);
    }

    public ContainerRequestFilterBuildItem(String className, Integer priority, Boolean preMatching) {
        this.className = className;
        this.priority = priority;
        this.preMatching = preMatching;
    }

    public String getClassName() {
        return className;
    }

    public Integer getPriority() {
        return priority;
    }

    public Boolean getPreMatching() {
        return preMatching;
    }
}
