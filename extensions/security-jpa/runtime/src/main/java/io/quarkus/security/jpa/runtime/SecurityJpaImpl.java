package io.quarkus.security.jpa.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.jpa.SecurityJpa;

public final class SecurityJpaImpl implements SecurityJpa {

    @Inject
    Instance<JpaIdentityProvider> jpaIdentityProvider;

    @Inject
    Instance<JpaTrustedIdentityProvider> jpaTrustedIdentityProvider;

    @Override
    public Collection<IdentityProvider<?>> getIdentityProviders() {
        final Collection<IdentityProvider<?>> result = new ArrayList<>();
        if (jpaIdentityProvider.isResolvable()) {
            result.add(jpaIdentityProvider.get());
        }
        if (jpaTrustedIdentityProvider.isResolvable()) {
            result.add(jpaTrustedIdentityProvider.get());
        }
        return Collections.unmodifiableCollection(result);
    }

}
