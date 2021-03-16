package io.quarkus.resteasy.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item used to register classes that are annotated with {@code @org.jboss.resteasy.reactive.server.ServerResponseFilter}
 */
public final class CustomContainerResponseFilterBuildItem extends MultiBuildItem {

    private final String className;

    public CustomContainerResponseFilterBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
