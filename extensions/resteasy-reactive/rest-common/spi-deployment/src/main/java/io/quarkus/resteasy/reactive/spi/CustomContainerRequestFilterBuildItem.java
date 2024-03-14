package io.quarkus.resteasy.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item used to register classes that are annotated with {@code @org.jboss.resteasy.reactive.server.ServerRequestFilter}
 */
public final class CustomContainerRequestFilterBuildItem extends MultiBuildItem {

    private final String className;

    public CustomContainerRequestFilterBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
