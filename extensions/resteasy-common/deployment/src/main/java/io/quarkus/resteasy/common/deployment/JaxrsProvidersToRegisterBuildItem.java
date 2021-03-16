package io.quarkus.resteasy.common.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class JaxrsProvidersToRegisterBuildItem extends SimpleBuildItem {

    private final Set<String> providers;
    private final Set<String> contributedProviders;
    private final Set<String> annotatedProviders;
    private final boolean useBuiltIn;

    public JaxrsProvidersToRegisterBuildItem(Set<String> providers, Set<String> contributedProviders,
            Set<String> annotatedProviders, boolean useBuiltIn) {
        this.providers = providers;
        this.contributedProviders = contributedProviders;
        this.annotatedProviders = annotatedProviders;
        this.useBuiltIn = useBuiltIn;
    }

    public Set<String> getProviders() {
        return this.providers;
    }

    public Set<String> getContributedProviders() {
        return this.contributedProviders;
    }

    public Set<String> getAnnotatedProviders() {
        return annotatedProviders;
    }

    public boolean useBuiltIn() {
        return useBuiltIn;
    }
}
