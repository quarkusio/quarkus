package io.quarkus.security.jpa.reactive.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.jpa.SecurityJpa;

public final class SecurityJpaReactiveImpl implements SecurityJpa {

    @Inject
    Instance<JpaReactiveIdentityProvider> jpaReactiveIdentityProvider;

    @Inject
    Instance<JpaReactiveTrustedIdentityProvider> jpaReactiveTrustedIdentityProvider;

    @Override
    public Collection<IdentityProvider<?>> getIdentityProviders() {
        final Collection<IdentityProvider<?>> result = new ArrayList<>();
        if (jpaReactiveIdentityProvider.isResolvable()) {
            result.add(jpaReactiveIdentityProvider.get());
        }
        if (jpaReactiveTrustedIdentityProvider.isResolvable()) {
            result.add(jpaReactiveTrustedIdentityProvider.get());
        }
        return Collections.unmodifiableCollection(result);
    }
}
