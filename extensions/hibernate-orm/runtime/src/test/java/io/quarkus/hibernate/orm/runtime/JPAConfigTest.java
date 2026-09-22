package io.quarkus.hibernate.orm.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProviderResolver;
import jakarta.persistence.spi.PersistenceProviderResolverHolder;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.Test;

class JPAConfigTest {

    @Test
    void failedPersistenceUnitStartIsNotRetried() {
        PersistenceProviderResolver originalResolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();
        PersistenceException initialFailure = new PersistenceException("initial persistence unit startup failure");
        AtomicInteger attempts = new AtomicInteger();
        HibernatePersistenceProvider provider = new HibernatePersistenceProvider() {
            @Override
            public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map<?, ?> properties) {
                if (attempts.incrementAndGet() > 1) {
                    throw new PersistenceException("retry hid the initial startup failure");
                }
                throw initialFailure;
            }
        };

        try {
            PersistenceProviderResolverHolder
                    .setPersistenceProviderResolver(new SingletonPersistenceProviderResolver(provider));
            JPAConfig.LazyPersistenceUnit persistenceUnit = new JPAConfig.LazyPersistenceUnit("test", false);

            assertThatThrownBy(persistenceUnit::get).isSameAs(initialFailure);
            assertThatThrownBy(persistenceUnit::get).isSameAs(initialFailure);
            assertThat(attempts).hasValue(1);
        } finally {
            PersistenceProviderResolverHolder.setPersistenceProviderResolver(originalResolver);
        }
    }
}
