package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

public final class ExtensionSslNativeSupportBuildItem extends MultiBuildItem {

    private String extension;

    public ExtensionSslNativeSupportBuildItem(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
