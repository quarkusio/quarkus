package io.quarkus.hibernate.orm.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.integrator.spi.Integrator;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.hibernate.orm.runtime.schema.SchemaManagementIntegrator;
import io.quarkus.hibernate.orm.runtime.session.ForwardingSession;
import io.quarkus.hibernate.orm.runtime.tenant.DataSourceTenantConnectionResolver;
import io.quarkus.runtime.annotations.Recorder;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@Recorder
public class HibernateOrmRecorder {

    private List<String> entities = new ArrayList<>();

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
            Scanner scanner, Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies proxyDefinitions) {
        SchemaManagementIntegrator.clearDsMap();
        for (QuarkusPersistenceUnitDefinition i : parsedPersistenceXmlDescriptors) {
            if (i.getDataSource().isPresent()) {
                SchemaManagementIntegrator.mapDatasource(i.getDataSource().get(), i.getName());
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

    public Supplier<JPAConfigSupport> jpaConfigSupportSupplier(JPAConfigSupport jpaConfigSupport) {
        return new Supplier<JPAConfigSupport>() {
            @Override
            public JPAConfigSupport get() {
                return jpaConfigSupport;
            }
        };
    }

    public Supplier<DataSourceTenantConnectionResolver> dataSourceTenantConnectionResolver(String persistenceUnitName,
            Optional<String> dataSourceName,
            MultiTenancyStrategy multiTenancyStrategy, String multiTenancySchemaDataSourceName) {
        return new Supplier<DataSourceTenantConnectionResolver>() {
            @Override
            public DataSourceTenantConnectionResolver get() {
                return new DataSourceTenantConnectionResolver(persistenceUnitName, dataSourceName, multiTenancyStrategy,
                        multiTenancySchemaDataSourceName);
            }
        };
    }

    public void startAllPersistenceUnits(BeanContainer beanContainer) {
        beanContainer.instance(JPAConfig.class).startAll();
    }

    public Supplier<SessionFactory> sessionFactorySupplier(String persistenceUnitName) {
        return new Supplier<SessionFactory>() {
            @Override
            public SessionFactory get() {
                SessionFactory sessionFactory = Arc.container().instance(JPAConfig.class).get()
                        .getEntityManagerFactory(persistenceUnitName)
                        .unwrap(SessionFactory.class);

                return sessionFactory;
            }
        };
    }

    public Supplier<Session> sessionSupplier(String persistenceUnitName) {
        return new Supplier<Session>() {
            @Override
            public Session get() {
                TransactionSessions transactionSessions = Arc.container()
                        .instance(TransactionSessions.class).get();
                ForwardingSession session = new ForwardingSession() {

                    @Override
                    protected Session delegate() {
                        return transactionSessions.getSession(persistenceUnitName);
                    }
                };
                return session;
            }
        };
    }

    public void doValidation(String puName) {
        Optional<String> val;
        if (puName.equals(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)) {
            val = ConfigProvider.getConfig().getOptionalValue("quarkus.hibernate-orm.database.generation", String.class);
        } else {
            val = ConfigProvider.getConfig().getOptionalValue("quarkus.hibernate-orm.\"" + puName + "\".database.generation",
                    String.class);
        }
        //if hibernate is already managing the schema we don't do this
        if (val.isPresent() && !val.get().equals("none")) {
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
