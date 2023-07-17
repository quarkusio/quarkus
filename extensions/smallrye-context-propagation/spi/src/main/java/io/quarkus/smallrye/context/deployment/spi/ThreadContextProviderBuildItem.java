package io.quarkus.smallrye.context.deployment.spi;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item can be used to register a {@link ThreadContextProvider}.
 */
public final class ThreadContextProviderBuildItem extends MultiBuildItem {

    private final Class<? extends ThreadContextProvider> provider;

    public ThreadContextProviderBuildItem(Class<? extends ThreadContextProvider> provider) {
        this.provider = provider;
    }

    public Class<? extends ThreadContextProvider> getProvider() {
        return provider;
    }

}
