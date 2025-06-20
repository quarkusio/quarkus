package io.quarkus.hibernate.reactive.runtime;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateReactiveRecorder {
    private final RuntimeValue<HibernateOrmRuntimeConfig> runtimeConfig;

    public HibernateReactiveRecorder(final RuntimeValue<HibernateOrmRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * The feature needs to be initialized, even if it's not enabled.
     *
     * @param enabled Set to false if it's not being enabled, to log appropriately.
     */
    public void callHibernateReactiveFeatureInit(boolean enabled) {
        HibernateReactive.featureInit(enabled);
    }

    public void initializePersistenceProvider(
            Map<String, List<HibernateOrmIntegrationRuntimeDescriptor>> integrationRuntimeDescriptors) {
        ReactivePersistenceProviderSetup.registerRuntimePersistenceProvider(runtimeConfig.getValue(),
                integrationRuntimeDescriptors);
    }

    public Function<SyntheticCreationalContext<Mutiny.SessionFactory>, Mutiny.SessionFactory> mutinySessionFactory(
            String persistenceUnitName) {
        return new Function<SyntheticCreationalContext<Mutiny.SessionFactory>, Mutiny.SessionFactory>() {
            @Override
            public Mutiny.SessionFactory apply(SyntheticCreationalContext<Mutiny.SessionFactory> context) {
                JPAConfig jpaConfig = context.getInjectedReference(JPAConfig.class);

                // This logic is already in JPAConfig, but we want to specialize the error message
                // If we don't do so the error message will say
                // "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit"
                // See io/quarkus/hibernate/orm/runtime/JPAConfig.getEntityManagerFactory:96
                if (jpaConfig.getDeactivatedPersistenceUnitNames()
                        .contains(HibernateReactive.DEFAULT_REACTIVE_PERSISTENCE_UNIT_NAME)) {
                    throw new IllegalStateException(
                            "Cannot retrieve the Mutiny.SessionFactory for persistence unit "
                                    + HibernateReactive.DEFAULT_REACTIVE_PERSISTENCE_UNIT_NAME
                                    + ": Hibernate Reactive was deactivated through configuration properties");
                }

                SessionFactory sessionFactory = jpaConfig
                        .getEntityManagerFactory(persistenceUnitName)
                        .unwrap(SessionFactory.class);

                return sessionFactory.unwrap(Mutiny.SessionFactory.class);
            }
        };
    }

}
