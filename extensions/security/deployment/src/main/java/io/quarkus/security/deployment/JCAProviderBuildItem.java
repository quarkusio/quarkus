package io.quarkus.security.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Metadata for the names of JCA {@linkplain java.security.Provider} to register for reflection
 */
public final class JCAProviderBuildItem extends MultiBuildItem {
    final private String providerName;
    final private String providerConfig;

    public JCAProviderBuildItem(String providerName) {
        this(providerName, null);
    }

    public JCAProviderBuildItem(String providerName, String providerConfig) {
        this.providerName = providerName;
        this.providerConfig = providerConfig;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getProviderConfig() {
        return providerConfig;
    }
}
