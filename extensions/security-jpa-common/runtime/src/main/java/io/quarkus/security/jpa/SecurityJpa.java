package io.quarkus.security.jpa;

import io.quarkus.arc.Arc;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.smallrye.common.annotation.Experimental;

/**
 * A CDI bean used to build Quarkus Security JPA {@link IdentityProvider}s programmatically.
 * This bean should be used together with the CDI event 'HttpSecurity' when you
 * want to configure the Basic or Form authentication to use the Quarkus Security {@link IdentityProvider}.
 */
@Experimental("This API is currently experimental and might get changed")
public interface SecurityJpa {

    /**
     * Creates new {@link IdentityProvider} for the {@link UsernamePasswordAuthenticationRequest} request.
     *
     * @param persistenceUnitName persistence unit name or null
     * @return new {@link IdentityProvider}
     */
    IdentityProvider<UsernamePasswordAuthenticationRequest> createJpaIdentityProvider(String persistenceUnitName);

    /**
     * Creates new {@link IdentityProvider} for the {@link TrustedAuthenticationRequest} request.
     *
     * @param persistenceUnitName persistence unit name or null
     * @return new {@link IdentityProvider}
     */
    IdentityProvider<TrustedAuthenticationRequest> createJpaTrustedIdentityProvider(String persistenceUnitName);

    /**
     * Creates new {@link IdentityProvider} for the {@link UsernamePasswordAuthenticationRequest} request and
     * the default persistence unit.
     *
     * @return new {@link IdentityProvider}
     */
    static IdentityProvider<UsernamePasswordAuthenticationRequest> jpa() {
        return getInstance().createJpaIdentityProvider(null);
    }

    /**
     * Creates new {@link IdentityProvider} for the {@link UsernamePasswordAuthenticationRequest} request.
     *
     * @param persistenceUnitName persistence unit name
     * @return new {@link IdentityProvider}
     */
    static IdentityProvider<UsernamePasswordAuthenticationRequest> jpa(String persistenceUnitName) {
        return getInstance().createJpaIdentityProvider(persistenceUnitName);
    }

    /**
     * Creates new {@link IdentityProvider} for the {@link TrustedAuthenticationRequest} request and the default
     * persistence unit.
     *
     * @return new {@link IdentityProvider}
     */
    static IdentityProvider<TrustedAuthenticationRequest> jpaTrustedProvider() {
        return getInstance().createJpaTrustedIdentityProvider(null);
    }

    /**
     * Creates new {@link IdentityProvider} for the {@link TrustedAuthenticationRequest} request.
     *
     * @param persistenceUnitName persistence unit name
     * @return new {@link IdentityProvider}
     */
    static IdentityProvider<TrustedAuthenticationRequest> jpaTrustedProvider(String persistenceUnitName) {
        return getInstance().createJpaTrustedIdentityProvider(persistenceUnitName);
    }

    private static SecurityJpa getInstance() {
        return Arc.requireContainer().select(SecurityJpa.class).get();
    }
}
