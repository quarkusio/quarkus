package io.quarkus.security.jpa.reactive.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.jpa.SecurityJpa;

public class SecurityJpaReactiveProvider {

    @Produces
    SecurityJpa createSecurityJpa(@Any Instance<Mutiny.SessionFactory> sessionFactoryInstance) {
        return new SecurityJpa() {
            @Override
            public IdentityProvider<UsernamePasswordAuthenticationRequest> createJpaIdentityProvider(
                    String persistenceUnitName) {
                var jpaIdentityProvider = newJpaIdentityProvider();
                jpaIdentityProvider.sessionFactory = getSessionFactoryInstance(persistenceUnitName);
                return jpaIdentityProvider;
            }

            @Override
            public IdentityProvider<TrustedAuthenticationRequest> createJpaTrustedIdentityProvider(String persistenceUnitName) {
                var trustedIdentityProvider = newJpaTrustedIdentityProvider();
                trustedIdentityProvider.sessionFactory = getSessionFactoryInstance(persistenceUnitName);
                return trustedIdentityProvider;
            }

            private Mutiny.SessionFactory getSessionFactoryInstance(String persistenceUnitName) {
                final Mutiny.SessionFactory sessionFactory;
                if (persistenceUnitName == null || DEFAULT_PERSISTENCE_UNIT_NAME.equals(persistenceUnitName)) {
                    if (!sessionFactoryInstance.isResolvable()) {
                        throw new IllegalArgumentException("Default persistence unit is not available");
                    }
                    sessionFactory = sessionFactoryInstance.get();
                } else {
                    var namedSessionFactoryInstance = sessionFactoryInstance
                            .select(new PersistenceUnit.PersistenceUnitLiteral(persistenceUnitName));
                    if (!namedSessionFactoryInstance.isResolvable()) {
                        throw new IllegalArgumentException("Unknown persistence unit '%s'".formatted(persistenceUnitName));
                    }
                    sessionFactory = namedSessionFactoryInstance.get();
                }
                return sessionFactory;
            }
        };
    }

    private static native JpaReactiveIdentityProvider newJpaIdentityProvider();

    private static native JpaReactiveTrustedIdentityProvider newJpaTrustedIdentityProvider();
}
