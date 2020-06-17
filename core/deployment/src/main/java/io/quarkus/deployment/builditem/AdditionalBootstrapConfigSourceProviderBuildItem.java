package io.quarkus.deployment.builditem;

import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item to use when an additional {@link ConfigSourceProvider} needs to be
 * registered to the Bootstrap Config setup.
 * This is needed because during Bootstrap Config setup, we don't auto-discover providers
 * but we do want for example the YAML provider to be enabled.
 */
public final class AdditionalBootstrapConfigSourceProviderBuildItem extends MultiBuildItem {

    private final String providerClassName;

    public AdditionalBootstrapConfigSourceProviderBuildItem(String providerClassName) {
        this.providerClassName = providerClassName;
    }

    public String getProviderClassName() {
        return providerClassName;
    }
}
