package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Indicates that a resource bundle should be included in the native image
 */
public final class NativeImageResourceBundleBuildItem extends MultiBuildItem {

    private final String bundleName;

    public NativeImageResourceBundleBuildItem(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getBundleName() {
        return bundleName;
    }
}
