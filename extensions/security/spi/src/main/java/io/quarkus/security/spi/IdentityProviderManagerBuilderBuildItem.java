package io.quarkus.security.spi;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;

/**
 * Provides a way for core extensions to build their own {@link IdentityProviderManager}.
 * Given identity providers must not be null or empty.
 */
public final class IdentityProviderManagerBuilderBuildItem extends SimpleBuildItem {

    private final Function<Collection<IdentityProvider<?>>, IdentityProviderManager> builder;

    public IdentityProviderManagerBuilderBuildItem(Function<Collection<IdentityProvider<?>>, IdentityProviderManager> builder) {
        this.builder = Objects.requireNonNull(builder);
    }

    public Function<Collection<IdentityProvider<?>>, IdentityProviderManager> getBuilder() {
        return builder;
    }
}
