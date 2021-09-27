package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * @deprecated please use {@link StaticInitConfigSourceProviderBuildItem} instead.
 */
@Deprecated
public final class AdditionalStaticInitConfigSourceProviderBuildItem extends MultiBuildItem {
    private final String providerClassName;

    public AdditionalStaticInitConfigSourceProviderBuildItem(String providerClassName) {
        this.providerClassName = providerClassName;
    }

    public String getProviderClassName() {
        return providerClassName;
    }
}
