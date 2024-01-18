package io.quarkus.restclient.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to ignore providers that were discovered via the common lookup mechanisms
 * but for one reason or another should not be applied to the rest client
 */
final class IgnoreClientProviderBuildItem extends MultiBuildItem {

    private final String providerClassName;

    public IgnoreClientProviderBuildItem(String providerClassName) {
        this.providerClassName = providerClassName;
    }

    public String getProviderClassName() {
        return providerClassName;
    }
}
