package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalStaticInitConfigSourceProviderBuildItem extends MultiBuildItem {
    private final String providerClassName;

    public AdditionalStaticInitConfigSourceProviderBuildItem(String providerClassName) {
        this.providerClassName = providerClassName;
    }

    public String getProviderClassName() {
        return providerClassName;
    }
}
