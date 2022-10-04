package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Indicates that a resource bundle should be included in the native image
 */
public final class NativeImageResourceBundleBuildItem extends MultiBuildItem {

    private final String bundleName;
    private final String moduleName;

    public NativeImageResourceBundleBuildItem(String bundleName) {
        this.bundleName = bundleName;
        this.moduleName = null;
    }

    public NativeImageResourceBundleBuildItem(String bundleName, String moduleName) {
        this.bundleName = bundleName;
        this.moduleName = moduleName;
    }

    public String getBundleName() {
        return bundleName;
    }

    public String getModuleName() {
        return moduleName;
    }
}
