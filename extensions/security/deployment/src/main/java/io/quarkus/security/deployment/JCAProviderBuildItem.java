package io.quarkus.security.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Metadata for the names of JCA {@linkplain java.security.Provider} to register for reflection
 */
public final class JCAProviderBuildItem extends MultiBuildItem {
    final private String providerName;
    final private List<String> providerConfigs;

    public JCAProviderBuildItem(String providerName) {
        this(providerName, List.of());
    }

    public JCAProviderBuildItem(String providerName, List<String> providerConfigs) {
        this.providerName = providerName;
        this.providerConfigs = providerConfigs != null ? providerConfigs : List.of();
    }

    public String getProviderName() {
        return providerName;
    }

    public List<String> getProviderConfigs() {
        return providerConfigs;
    }
}
