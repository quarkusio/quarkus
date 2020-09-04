package io.quarkus.resteasy.common.deployment;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class JaxrsProvidersToRegisterBuildItem extends SimpleBuildItem {

    private final Set<String> providers;
    private final Set<String> contributedProviders;
    private final Set<String> contributedProvidersWithoutResteasy;
    private final boolean useBuiltIn;

    public JaxrsProvidersToRegisterBuildItem(Set<String> providers, Set<String> contributedProviders, boolean useBuiltIn) {
        this.providers = providers;
        this.contributedProviders = contributedProviders;
        // extensions can end up contributing RESTEasy providers, so we like to filter them out when necessary
        // in order to avoid 'RESTEASY002155: Provider class ... '
        this.contributedProvidersWithoutResteasy = new HashSet<>();
        for (String contributedProvider : contributedProviders) {
            if (!contributedProvider.startsWith(ResteasyCommonProcessor.RESTEASY_PROVIDERS_BASE_PACKAGE)) {
                contributedProvidersWithoutResteasy.add(contributedProvider);
            }
        }
        this.useBuiltIn = useBuiltIn;
    }

    public Set<String> getProviders() {
        return this.providers;
    }

    public Set<String> getContributedProviders() {
        return this.contributedProviders;
    }

    public Set<String> getContributedProvidersWithoutResteasy() {
        return this.contributedProvidersWithoutResteasy;
    }

    public boolean useBuiltIn() {
        return useBuiltIn;
    }
}
