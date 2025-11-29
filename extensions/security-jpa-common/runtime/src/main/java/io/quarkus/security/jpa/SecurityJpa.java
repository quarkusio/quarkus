package io.quarkus.security.jpa;

import java.util.Collection;

import io.quarkus.arc.Arc;
import io.quarkus.security.identity.IdentityProvider;

/**
 * A CDI beans used to retrieve generated Quarkus Security JPA beans.
 */
public interface SecurityJpa {

    /**
     * @return Quarkus Security JPA {@link IdentityProvider}s
     */
    Collection<IdentityProvider<?>> getIdentityProviders();

    /**
     * Looks up the {@link SecurityJpa} CDI bean and returns the Quarkus Security JPA {@link IdentityProvider}s.
     *
     * @return Quarkus Security JPA {@link IdentityProvider}s
     */
    static Collection<IdentityProvider<?>> jpa() {
        try (var securityJpaInstance = Arc.requireContainer().instance(SecurityJpa.class)) {
            return securityJpaInstance.get().getIdentityProviders();
        }
    }
}
