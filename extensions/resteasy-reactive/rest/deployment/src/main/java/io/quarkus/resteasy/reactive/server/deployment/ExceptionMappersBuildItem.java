package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.server.core.ExceptionMapping;

import io.quarkus.builder.item.SimpleBuildItem;

final class ExceptionMappersBuildItem extends SimpleBuildItem {

    private final ExceptionMapping exceptionMapping;

    public ExceptionMappersBuildItem(ExceptionMapping exceptionMapping) {
        this.exceptionMapping = exceptionMapping;
    }

    public ExceptionMapping getExceptionMapping() {
        return exceptionMapping;
    }
}
