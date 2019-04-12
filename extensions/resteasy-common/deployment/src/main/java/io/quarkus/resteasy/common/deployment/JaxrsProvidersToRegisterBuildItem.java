package io.quarkus.resteasy.common.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class JaxrsProvidersToRegisterBuildItem extends SimpleBuildItem {

    private final Set<String> providers;
    private final Set<String> contributedProviders;
    private final boolean useBuiltIn;

    public JaxrsProvidersToRegisterBuildItem(Set<String> providers, Set<String> contributedProviders, boolean useBuiltIn) {
        this.providers = providers;
        this.contributedProviders = contributedProviders;
        this.useBuiltIn = useBuiltIn;
    }

    public Set<String> getProviders() {
        return this.providers;
    }

    public Set<String> getContributedProviders() {
        return this.contributedProviders;
    }

    public boolean useBuiltIn() {
        return useBuiltIn;
    }
}
