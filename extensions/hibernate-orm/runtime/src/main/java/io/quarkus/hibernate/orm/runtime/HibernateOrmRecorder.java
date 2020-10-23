package io.quarkus.hibernate.orm.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.integrator.spi.Integrator;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.entitymanager.ForwardingEntityManager;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
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

    public void setupPersistenceProvider(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig) {
        PersistenceProviderSetup.registerRuntimePersistenceProvider(hibernateOrmRuntimeConfig);
    }

    public BeanContainerListener initMetadata(List<QuarkusPersistenceUnitDefinition> parsedPersistenceXmlDescriptors,
            Scanner scanner, Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies proxyDefinitions) {
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
            String dataSourceName,
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

    public Supplier<EntityManagerFactory> entityManagerFactorySupplier(String persistenceUnitName) {
        return new Supplier<EntityManagerFactory>() {

            @Override
            public EntityManagerFactory get() {
                EntityManagerFactory entityManagerFactory = Arc.container().instance(JPAConfig.class).get()
                        .getEntityManagerFactory(persistenceUnitName);

                return entityManagerFactory;
            }
        };
    }

    public Supplier<EntityManager> entityManagerSupplier(String persistenceUnitName) {
        return new Supplier<EntityManager>() {
            @Override
            public EntityManager get() {
                TransactionEntityManagers transactionEntityManagers = Arc.container()
                        .instance(TransactionEntityManagers.class).get();
                ForwardingEntityManager entityManager = new ForwardingEntityManager() {

                    @Override
                    protected EntityManager delegate() {
                        return transactionEntityManagers.getEntityManager(persistenceUnitName);
                    }
                };
                return entityManager;
            }
        };
    }

}
