package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.value.registry.RuntimeInfoProvider;

public final class ValueRegistryRuntimeInfoProviderBuildItem extends MultiBuildItem {
    private final Class<? extends RuntimeInfoProvider> runtimeInfoProvider;

    public ValueRegistryRuntimeInfoProviderBuildItem(Class<? extends RuntimeInfoProvider> runtimeInfoProvider) {
        this.runtimeInfoProvider = runtimeInfoProvider;
    }

    public Class<? extends RuntimeInfoProvider> getRuntimeInfoProvider() {
        return runtimeInfoProvider;
    }
}
