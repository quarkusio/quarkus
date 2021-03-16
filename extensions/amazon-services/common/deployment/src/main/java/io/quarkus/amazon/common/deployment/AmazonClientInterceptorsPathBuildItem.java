package io.quarkus.amazon.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class AmazonClientInterceptorsPathBuildItem extends MultiBuildItem {
    private final String interceptorsPath;

    public AmazonClientInterceptorsPathBuildItem(String interceptorsPath) {
        this.interceptorsPath = interceptorsPath;
    }

    public String getInterceptorsPath() {
        return interceptorsPath;
    }
}
