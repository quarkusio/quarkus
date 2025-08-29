package io.quarkus.hibernate.reactive.runtime;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;
import io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil;
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

    public Supplier<ActiveResult> checkActiveSupplier(String puName, Optional<String> dataSourceName,
            Set<String> entityClassNames) {
        return new Supplier<>() {
            @Override
            public ActiveResult get() {
                Optional<Boolean> active = runtimeConfig.getValue().persistenceUnits().get(puName).active();
                if (active.isPresent() && !active.get()) {
                    return ActiveResult.inactive(
                            PersistenceUnitUtil.persistenceUnitInactiveReasonDeactivated(puName, dataSourceName));
                }

                if (entityClassNames.isEmpty() && dataSourceName.isPresent()) {
                    // Persistence units are inactive when the corresponding datasource is inactive.
                    var dataSourceBean = ReactiveDataSourceUtil.dataSourceInstance(dataSourceName.get()).getHandle().getBean();
                    var dataSourceActive = dataSourceBean.checkActive();
                    if (!dataSourceActive.value()) {
                        return ActiveResult.inactive(
                                String.format(Locale.ROOT,
                                        "Persistence unit '%s' was deactivated automatically because it doesn't include any entity type and its datasource '%s' was deactivated.",
                                        puName,
                                        dataSourceName.get()),
                                dataSourceActive);
                    }
                }

                return ActiveResult.active();
            }
        };
    }

    public Function<SyntheticCreationalContext<Mutiny.SessionFactory>, Mutiny.SessionFactory> mutinySessionFactory(
            String persistenceUnitName) {
        return new Function<SyntheticCreationalContext<Mutiny.SessionFactory>, Mutiny.SessionFactory>() {
            @Override
            public Mutiny.SessionFactory apply(SyntheticCreationalContext<Mutiny.SessionFactory> context) {
                JPAConfig jpaConfig = context.getInjectedReference(JPAConfig.class);

                SessionFactory sessionFactory = jpaConfig
                        .getEntityManagerFactory(persistenceUnitName, true)
                        .unwrap(SessionFactory.class);

                return sessionFactory.unwrap(Mutiny.SessionFactory.class);
            }
        };
    }

}
