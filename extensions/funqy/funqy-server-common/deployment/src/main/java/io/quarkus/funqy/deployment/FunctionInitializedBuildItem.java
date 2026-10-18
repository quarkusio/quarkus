package io.quarkus.funqy.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class FunctionInitializedBuildItem extends SimpleBuildItem {
    public static final FunctionInitializedBuildItem SINGLETON = new FunctionInitializedBuildItem();

    private FunctionInitializedBuildItem() {
    }
}
