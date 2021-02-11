package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Feature;

public final class ExtensionSslNativeSupportBuildItem extends MultiBuildItem {

    private String extension;

    public ExtensionSslNativeSupportBuildItem(Feature feature) {
        this(feature.getName());
    }

    public ExtensionSslNativeSupportBuildItem(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
