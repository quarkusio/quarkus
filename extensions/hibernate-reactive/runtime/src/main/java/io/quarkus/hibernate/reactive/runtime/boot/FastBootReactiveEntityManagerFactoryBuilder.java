package io.quarkus.hibernate.reactive.runtime.boot;

import javax.persistence.EntityManagerFactory;

import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.reactive.bulk.impl.ReactiveBulkIdStrategy;
import org.hibernate.reactive.session.impl.ReactiveSessionFactoryImpl;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.RuntimeSettings;
import io.quarkus.hibernate.orm.runtime.boot.FastBootEntityManagerFactoryBuilder;
import io.quarkus.hibernate.orm.runtime.recording.PrevalidatedQuarkusMetadata;

public final class FastBootReactiveEntityManagerFactoryBuilder extends FastBootEntityManagerFactoryBuilder {

    public FastBootReactiveEntityManagerFactoryBuilder(PrevalidatedQuarkusMetadata metadata, String persistenceUnitName,
            StandardServiceRegistry standardServiceRegistry, RuntimeSettings runtimeSettings, Object validatorFactory,
            Object cdiBeanManager) {
        super(metadata, persistenceUnitName, standardServiceRegistry, runtimeSettings, validatorFactory, cdiBeanManager);
    }

    @Override
    public EntityManagerFactory build() {
        final SessionFactoryOptionsBuilder optionsBuilder = metadata.buildSessionFactoryOptionsBuilder();
        optionsBuilder.enableCollectionInDefaultFetchGroup(true);
        optionsBuilder.applyMultiTableBulkIdStrategy(new ReactiveBulkIdStrategy(metadata));
        populate(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, optionsBuilder, standardServiceRegistry);
        SessionFactoryOptions options = optionsBuilder.buildOptions();
        return new ReactiveSessionFactoryImpl(metadata, options);
    }
}
