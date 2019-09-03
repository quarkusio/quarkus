package io.quarkus.security.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Metadata for the names of JCA {@linkplain java.security.Provider} to register for reflection
 */
public final class JCAProviderBuildItem extends MultiBuildItem {
    private String providerName;

    public JCAProviderBuildItem(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}
