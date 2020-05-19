package io.quarkus.hibernate.reactive.runtime.boot;

import javax.persistence.EntityManagerFactory;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.reactive.boot.impl.ReactiveSessionFactoryOptions;
import org.hibernate.reactive.impl.StageSessionFactoryImpl;

import io.quarkus.hibernate.orm.runtime.RuntimeSettings;
import io.quarkus.hibernate.orm.runtime.boot.FastBootEntityManagerFactoryBuilder;
import io.quarkus.hibernate.orm.runtime.recording.PrevalidatedQuarkusMetadata;

public final class FastBootReactiveEntityManagerFactoryBuilder extends FastBootEntityManagerFactoryBuilder {

    public FastBootReactiveEntityManagerFactoryBuilder(
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
            ReactiveSessionFactoryOptions options = new ReactiveSessionFactoryOptions(optionsBuilder.buildOptions());
            return new StageSessionFactoryImpl(metadata.getOriginalMetadata(), options);
        } catch (Exception e) {
            throw persistenceException("Unable to build Hibernate Reactive SessionFactory", e);
        }
    }
}
