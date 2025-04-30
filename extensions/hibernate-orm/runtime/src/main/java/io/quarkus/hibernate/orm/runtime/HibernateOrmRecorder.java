package io.quarkus.hibernate.orm.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.engine.spi.SessionLazyDelegator;
import org.hibernate.integrator.spi.Integrator;
import org.jboss.logging.Logger;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.hibernate.orm.runtime.schema.SchemaManagementIntegrator;
import io.quarkus.hibernate.orm.runtime.tenant.DataSourceTenantConnectionResolver;
import io.quarkus.runtime.annotations.Recorder;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@Recorder
public class HibernateOrmRecorder {

    private final PreGeneratedProxies proxyDefinitions;
    private final List<String> entities = new ArrayList<>();

    @Inject
    public HibernateOrmRecorder(PreGeneratedProxies proxyDefinitions) {
        this.proxyDefinitions = proxyDefinitions;
    }

    public void enlistPersistenceUnit(Set<String> entityClassNames) {
        entities.addAll(entityClassNames);
        Logger.getLogger("io.quarkus.hibernate.orm").debugf("List of entities found by Quarkus deployment:%n%s", entities);
    }

    /**
     * The feature needs to be initialized, even if it's not enabled.
     *
     * @param enabled Set to false if it's not being enabled, to log appropriately.
     */
    public void callHibernateFeatureInit(boolean enabled) {
        Hibernate.featureInit(enabled);
    }

    public void setupPersistenceProvider(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig,
            Map<String, List<HibernateOrmIntegrationRuntimeDescriptor>> integrationRuntimeDescriptors) {
        PersistenceProviderSetup.registerRuntimePersistenceProvider(hibernateOrmRuntimeConfig, integrationRuntimeDescriptors);
    }

    public BeanContainerListener initMetadata(List<QuarkusPersistenceUnitDefinition> parsedPersistenceXmlDescriptors,
            Scanner scanner, Collection<Class<? extends Integrator>> additionalIntegrators) {
        SchemaManagementIntegrator.clearDsMap();
        for (QuarkusPersistenceUnitDefinition i : parsedPersistenceXmlDescriptors) {
            if (i.getConfig().getDataSource().isPresent()) {
                SchemaManagementIntegrator.mapDatasource(i.getConfig().getDataSource().get(), i.getName());
            }
        }
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                PersistenceUnitsHolder.initializeJpa(parsedPersistenceXmlDescriptors, scanner, additionalIntegrators,
                        proxyDefinitions);
            }
        };
    }

    public Supplier<DataSourceTenantConnectionResolver> dataSourceTenantConnectionResolver(String persistenceUnitName,
            Optional<String> dataSourceName,
            MultiTenancyStrategy multiTenancyStrategy) {
        return new Supplier<DataSourceTenantConnectionResolver>() {
            @Override
            public DataSourceTenantConnectionResolver get() {
                return new DataSourceTenantConnectionResolver(persistenceUnitName, dataSourceName, multiTenancyStrategy);
            }
        };
    }

    public Supplier<JPAConfig> jpaConfigSupplier(HibernateOrmRuntimeConfig config) {
        return () -> new JPAConfig(config);
    }

    public void startAllPersistenceUnits(BeanContainer beanContainer) {
        beanContainer.beanInstance(JPAConfig.class).startAll();
    }

    public Function<SyntheticCreationalContext<SessionFactory>, SessionFactory> sessionFactorySupplier(
            String persistenceUnitName) {
        return new Function<SyntheticCreationalContext<SessionFactory>, SessionFactory>() {
            @Override
            public SessionFactory apply(SyntheticCreationalContext<SessionFactory> context) {
                SessionFactory sessionFactory = context.getInjectedReference(JPAConfig.class)
                        .getEntityManagerFactory(persistenceUnitName)
                        .unwrap(SessionFactory.class);

                return sessionFactory;
            }
        };
    }

    public Function<SyntheticCreationalContext<Session>, Session> sessionSupplier(String persistenceUnitName) {
        return new Function<SyntheticCreationalContext<Session>, Session>() {

            @Override
            public Session apply(SyntheticCreationalContext<Session> context) {
                TransactionSessions transactionSessions = context.getInjectedReference(TransactionSessions.class);
                return new SessionLazyDelegator(new Supplier<Session>() {
                    @Override
                    public Session get() {
                        return transactionSessions.getSession(persistenceUnitName);
                    }
                });
            }
        };
    }

    public Function<SyntheticCreationalContext<StatelessSession>, StatelessSession> statelessSessionSupplier(
            String persistenceUnitName) {
        return new Function<SyntheticCreationalContext<StatelessSession>, StatelessSession>() {

            @Override
            public StatelessSession apply(SyntheticCreationalContext<StatelessSession> context) {
                TransactionSessions transactionSessions = context.getInjectedReference(TransactionSessions.class);
                return new StatelessSessionLazyDelegator(new Supplier<StatelessSession>() {
                    @Override
                    public StatelessSession get() {
                        return transactionSessions.getStatelessSession(persistenceUnitName);
                    }
                });
            }
        };
    }

    public void doValidation(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig, String puName) {
        HibernateOrmRuntimeConfigPersistenceUnit hibernateOrmRuntimeConfigPersistenceUnit = hibernateOrmRuntimeConfig
                .persistenceUnits().get(puName);
        String schemaManagementStrategy = hibernateOrmRuntimeConfigPersistenceUnit.database().generation().generation()
                .orElse(hibernateOrmRuntimeConfigPersistenceUnit.schemaManagement().strategy());

        //if hibernate is already managing the schema we don't do this
        if (!"none".equals(schemaManagementStrategy)) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                SchemaManagementIntegrator.runPostBootValidation(puName);
            }
        }, "Hibernate post-boot validation thread for " + puName).start();
    }
}
