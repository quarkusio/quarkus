package io.quarkus.deployment.builditem.substrate;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Indicates that a resource bundle should be included in the native image
 * 
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem
 *             NativeImageResourceBundleBuildItem} instead.
 */
@Deprecated
public final class SubstrateResourceBundleBuildItem extends MultiBuildItem {

    private final String bundleName;

    public SubstrateResourceBundleBuildItem(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getBundleName() {
        return bundleName;
    }
}
