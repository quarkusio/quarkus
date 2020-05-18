package io.quarkus.hibernate.rx.runtime.boot;

import javax.persistence.EntityManagerFactory;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.rx.boot.impl.RxSessionFactoryOptions;
import org.hibernate.rx.engine.impl.RxSessionFactoryImpl;

import io.quarkus.hibernate.orm.runtime.RuntimeSettings;
import io.quarkus.hibernate.orm.runtime.boot.FastBootEntityManagerFactoryBuilder;
import io.quarkus.hibernate.orm.runtime.recording.PrevalidatedQuarkusMetadata;

public final class FastBootRxEntityManagerFactoryBuilder extends FastBootEntityManagerFactoryBuilder {

    public FastBootRxEntityManagerFactoryBuilder(
            PrevalidatedQuarkusMetadata metadata, String persistenceUnitName,
            StandardServiceRegistry standardServiceRegistry, RuntimeSettings runtimeSettings, Object validatorFactory,
            Object cdiBeanManager, MultiTenancyStrategy strategy) {
        super(metadata, persistenceUnitName, standardServiceRegistry, runtimeSettings, validatorFactory, cdiBeanManager,
                strategy);
    }

    @Override
    public EntityManagerFactory build() {
        try {
            final SessionFactoryOptionsBuilder optionsBuilder = metadata.buildSessionFactoryOptionsBuilder();
            populate(optionsBuilder, standardServiceRegistry, multiTenancyStrategy);
            RxSessionFactoryOptions options = new RxSessionFactoryOptions(optionsBuilder.buildOptions());
            return new RxSessionFactoryImpl(metadata.getOriginalMetadata(), options);
        } catch (Exception e) {
            throw persistenceException("Unable to build Hibernate SessionFactory", e);
        }
    }
}
