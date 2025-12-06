package io.quarkus.security.spi.runtime;

import java.util.Collection;

import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentityAugmentor;

/**
 * CDI bean that enables Quarkus core extensions to create own {@link IdentityProviderManager}
 * from the global {@link IdentityProviderManager} used by Quarkus.
 */
public interface IdentityProviderManagerBuilder {

    /**
     * Create a new {@link IdentityProviderManager}.
     *
     * @param identityProviders {@link IdentityProvider}s; if specified, globally configured {@link IdentityProvider}s
     *        will be ignored
     * @param securityIdentityAugmentors {@link SecurityIdentityAugmentor}s; if specified, globally configured
     *        {@link SecurityIdentityAugmentor} will be ignored
     * @return the new {@link IdentityProviderManager}
     */
    IdentityProviderManager build(Collection<IdentityProvider<?>> identityProviders,
            Collection<SecurityIdentityAugmentor> securityIdentityAugmentors);

}
