package io.quarkus.security.jpa.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import org.hibernate.SessionFactory;

import io.quarkus.hibernate.orm.PersistenceUnit.PersistenceUnitLiteral;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.jpa.SecurityJpa;

public class SecurityJpaProvider {

    @Produces
    SecurityJpa createSecurityJpa(@Any Instance<SessionFactory> sessionFactoryInstance) {
        return new SecurityJpa() {

            private String persistenceUnitName = DEFAULT_PERSISTENCE_UNIT_NAME;
            private final List<SecurityIdentityAugmentor> augmentors = new ArrayList<>();

            @Override
            public Collection<IdentityProvider<?>> identityProviders() {
                final SessionFactory sessionFactory;
                if (DEFAULT_PERSISTENCE_UNIT_NAME.equals(persistenceUnitName)) {
                    sessionFactory = sessionFactoryInstance.get();
                } else {
                    sessionFactory = getNamedSessionFactoryInstance().get();
                }
                var jpaIdentityProvider = newJpaIdentityProvider();
                jpaIdentityProvider.sessionFactory = sessionFactory;
                var trustedIdentityProvider = newJpaTrustedIdentityProvider();
                trustedIdentityProvider.sessionFactory = sessionFactory;

                return List.of(jpaIdentityProvider, trustedIdentityProvider);
            }

            @Override
            public Collection<SecurityIdentityAugmentor> securityIdentityAugmentors() {
                return List.copyOf(augmentors);
            }

            private Instance<SessionFactory> getNamedSessionFactoryInstance() {
                return sessionFactoryInstance.select(new PersistenceUnitLiteral(persistenceUnitName));
            }

            @Override
            public SecurityJpa persistence(String persistenceUnitName) {
                this.persistenceUnitName = Objects.requireNonNull(persistenceUnitName);
                if (!getNamedSessionFactoryInstance().isResolvable()) {
                    throw new IllegalArgumentException("Unknown persistence unit '%s'".formatted(persistenceUnitName));
                }
                return this;
            }

            @Override
            public SecurityJpa augmentor(SecurityIdentityAugmentor securityIdentityAugmentor) {
                augmentors.add(Objects.requireNonNull(securityIdentityAugmentor));
                return this;
            }
        };
    }

    private static native JpaIdentityProvider newJpaIdentityProvider();

    private static native JpaTrustedIdentityProvider newJpaTrustedIdentityProvider();

}
