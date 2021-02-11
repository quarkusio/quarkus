package io.quarkus.smallrye.graphql.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

final class SmallRyeGraphQLInitializedBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Boolean> initialized;

    public SmallRyeGraphQLInitializedBuildItem(RuntimeValue<Boolean> initialized) {
        this.initialized = initialized;
    }

    public RuntimeValue<Boolean> getInitialized() {
        return initialized;
    }
}