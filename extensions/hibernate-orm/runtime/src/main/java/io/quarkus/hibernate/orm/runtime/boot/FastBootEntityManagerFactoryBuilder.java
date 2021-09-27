package io.quarkus.hibernate.orm.runtime.boot;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.internal.SessionFactoryObserverForBytecodeEnhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.RuntimeSettings;
import io.quarkus.hibernate.orm.runtime.recording.PrevalidatedQuarkusMetadata;
import io.quarkus.hibernate.orm.runtime.tenant.HibernateCurrentTenantIdentifierResolver;

public class FastBootEntityManagerFactoryBuilder implements EntityManagerFactoryBuilder {

    protected final PrevalidatedQuarkusMetadata metadata;
    protected final String persistenceUnitName;
    protected final StandardServiceRegistry standardServiceRegistry;
    private final RuntimeSettings runtimeSettings;
    private final Object validatorFactory;
    private final Object cdiBeanManager;

    public FastBootEntityManagerFactoryBuilder(
            PrevalidatedQuarkusMetadata metadata, String persistenceUnitName,
            StandardServiceRegistry standardServiceRegistry, RuntimeSettings runtimeSettings, Object validatorFactory,
            Object cdiBeanManager) {
        this.metadata = metadata;
        this.persistenceUnitName = persistenceUnitName;
        this.standardServiceRegistry = standardServiceRegistry;
        this.runtimeSettings = runtimeSettings;
        this.validatorFactory = validatorFactory;
        this.cdiBeanManager = cdiBeanManager;
    }

    @Override
    public EntityManagerFactoryBuilder withValidatorFactory(Object validatorFactory) {
        return null;
    }

    @Override
    public EntityManagerFactoryBuilder withDataSource(DataSource dataSource) {
        return null;
    }

    @Override
    public EntityManagerFactory build() {
        try {
            final SessionFactoryOptionsBuilder optionsBuilder = metadata.buildSessionFactoryOptionsBuilder();
            populate(persistenceUnitName, optionsBuilder, standardServiceRegistry);
            return new SessionFactoryImpl(metadata, optionsBuilder.buildOptions(), HQLQueryPlan::new);
        } catch (Exception e) {
            throw persistenceException("Unable to build Hibernate SessionFactory", e);
        }
    }

    @Override
    public void cancel() {
        // nothing?
    }

    @Override
    public void generateSchema() {
        try {
            SchemaManagementToolCoordinator.process(metadata, standardServiceRegistry, runtimeSettings.getSettings(),
                    DelayedDropRegistryNotAvailableImpl.INSTANCE);
        } catch (Exception e) {
            throw persistenceException("Error performing schema management", e);
        }

        // release this builder
        cancel();
    }

    protected PersistenceException persistenceException(String message, Exception cause) {
        // Provide a comprehensible message if there is an issue with SSL support
        Throwable t = cause;
        while (t != null) {
            if (t instanceof NoSuchAlgorithmException) {
                message += "Unable to enable SSL support. You might be in the case where you used the `quarkus.ssl.native=false` configuration"
                        + " and SSL was not disabled automatically for your driver.";
                break;
            }

            if (t instanceof CommandAcceptanceException) {
                message = "Invalid import file. Make sure your statements are valid and properly separated by a semi-colon.";
                break;
            }

            t = t.getCause();
        }

        return new PersistenceException(getExceptionHeader() + message, cause);
    }

    private String getExceptionHeader() {
        return "[PersistenceUnit: " + persistenceUnitName + "] ";
    }

    protected void populate(String persistenceUnitName, SessionFactoryOptionsBuilder options, StandardServiceRegistry ssr) {

        // will use user override value or default to false if not supplied to follow
        // JPA spec.
        final boolean jtaTransactionAccessEnabled = runtimeSettings.getBoolean(
                AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS);
        if (!jtaTransactionAccessEnabled) {
            options.disableJtaTransactionAccess();
        }

        final boolean allowRefreshDetachedEntity = runtimeSettings.getBoolean(
                org.hibernate.cfg.AvailableSettings.ALLOW_REFRESH_DETACHED_ENTITY);
        if (!allowRefreshDetachedEntity) {
            options.disableRefreshDetachedEntity();
        }

        // Locate and apply any requested SessionFactoryObserver
        final Object sessionFactoryObserverSetting = runtimeSettings.get(AvailableSettings.SESSION_FACTORY_OBSERVER);
        if (sessionFactoryObserverSetting != null) {

            final StrategySelector strategySelector = ssr.getService(StrategySelector.class);
            final SessionFactoryObserver suppliedSessionFactoryObserver = strategySelector
                    .resolveStrategy(SessionFactoryObserver.class, sessionFactoryObserverSetting);
            options.addSessionFactoryObservers(suppliedSessionFactoryObserver);
        }

        options.addSessionFactoryObservers(new ServiceRegistryCloser());

        options.applyEntityNotFoundDelegate(new JpaEntityNotFoundDelegate());

        if (this.validatorFactory != null) {
            options.applyValidatorFactory(validatorFactory);
        }
        if (this.cdiBeanManager != null) {
            options.applyBeanManager(cdiBeanManager);
        }

        //Small memory optimisations: ensure the class transformation caches of the bytecode enhancer
        //are cleared both on start and on close of the SessionFactory.
        //(On start is useful especially in Quarkus as we won't do any more enhancement after this point)
        BytecodeProvider bytecodeProvider = ssr.getService(BytecodeProvider.class);
        options.addSessionFactoryObservers(new SessionFactoryObserverForBytecodeEnhancer(bytecodeProvider));

        if (options.getMultiTenancyStrategy() != MultiTenancyStrategy.NONE) {
            options.applyCurrentTenantIdentifierResolver(new HibernateCurrentTenantIdentifierResolver(persistenceUnitName));
        }

        InjectableInstance<Interceptor> interceptorInstance = PersistenceUnitUtil.singleExtensionInstanceForPersistenceUnit(
                Interceptor.class, persistenceUnitName);
        if (!interceptorInstance.isUnsatisfied()) {
            options.applyStatelessInterceptorSupplier(interceptorInstance::get);
        }
    }

    private static class ServiceRegistryCloser implements SessionFactoryObserver {

        @Override
        public void sessionFactoryCreated(SessionFactory sessionFactory) {
            // nothing to do
        }

        @Override
        public void sessionFactoryClosed(SessionFactory sessionFactory) {
            SessionFactoryImplementor sfi = ((SessionFactoryImplementor) sessionFactory);
            sfi.getServiceRegistry().destroy();
            ServiceRegistry basicRegistry = sfi.getServiceRegistry().getParentServiceRegistry();
            ((ServiceRegistryImplementor) basicRegistry).destroy();
        }
    }

    private static class JpaEntityNotFoundDelegate implements EntityNotFoundDelegate, Serializable {

        @Override
        public void handleEntityNotFound(String entityName, Serializable id) {
            throw new EntityNotFoundException("Unable to find " + entityName + " with id " + id);
        }
    }

}
