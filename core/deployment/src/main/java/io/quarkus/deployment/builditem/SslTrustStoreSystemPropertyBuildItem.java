package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

public final class SslTrustStoreSystemPropertyBuildItem extends SimpleBuildItem {

    private final String path;

    public SslTrustStoreSystemPropertyBuildItem(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
