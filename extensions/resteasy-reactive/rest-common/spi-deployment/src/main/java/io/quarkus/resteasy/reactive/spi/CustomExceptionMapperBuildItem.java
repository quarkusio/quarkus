package io.quarkus.resteasy.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item used to register classes that are annotated with {@code @org.jboss.resteasy.reactive.server.ServerExceptionMapper}
 */
public final class CustomExceptionMapperBuildItem extends MultiBuildItem {

    private final String className;

    public CustomExceptionMapperBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
