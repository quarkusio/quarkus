package io.quarkus.resteasy.reactive.common.deployment;

import java.util.function.Consumer;

import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;

import io.quarkus.builder.item.MultiBuildItem;

public final class ResourceInterceptorsContributorBuildItem extends MultiBuildItem {

    private final Consumer<ResourceInterceptors> buildTask;

    public ResourceInterceptorsContributorBuildItem(Consumer<ResourceInterceptors> function) {
        this.buildTask = function;
    }

    public Consumer<ResourceInterceptors> getBuildTask() {
        return buildTask;
    }
}
