package io.quarkus.resteasy.reactive.common.deployment;

import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ResourceInterceptorsBuildItem extends SimpleBuildItem {

    private final ResourceInterceptors resourceInterceptors;

    public ResourceInterceptorsBuildItem(ResourceInterceptors resourceInterceptors) {
        this.resourceInterceptors = resourceInterceptors;
    }

    public ResourceInterceptors getResourceInterceptors() {
        return resourceInterceptors;
    }
}
