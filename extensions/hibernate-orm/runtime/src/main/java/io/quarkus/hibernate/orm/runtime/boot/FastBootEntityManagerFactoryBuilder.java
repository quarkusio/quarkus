package io.quarkus.hibernate.orm.runtime.boot;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.bytecode.internal.SessionFactoryObserverForBytecodeEnhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.type.format.FormatMapper;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.JsonFormat;
import io.quarkus.hibernate.orm.XmlFormat;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.RuntimeSettings;
import io.quarkus.hibernate.orm.runtime.customized.BuiltinFormatMapperBehaviour;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.hibernate.orm.runtime.observers.QuarkusSessionFactoryObserverForDbVersionCheck;
import io.quarkus.hibernate.orm.runtime.observers.SessionFactoryObserverForNamedQueryValidation;
import io.quarkus.hibernate.orm.runtime.observers.SessionFactoryObserverForSchemaExport;
import io.quarkus.hibernate.orm.runtime.recording.PrevalidatedQuarkusMetadata;
import io.quarkus.hibernate.orm.runtime.tenant.HibernateCurrentTenantIdentifierResolver;

public class FastBootEntityManagerFactoryBuilder implements EntityManagerFactoryBuilder {

    protected final QuarkusPersistenceUnitDescriptor puDescriptor;
    protected final PrevalidatedQuarkusMetadata metadata;
    protected final StandardServiceRegistry standardServiceRegistry;
    private final RuntimeSettings runtimeSettings;
    private final Object validatorFactory;
    private final Object cdiBeanManager;
    private final BuiltinFormatMapperBehaviour builtinFormatMapperBehaviour;

    protected final MultiTenancyStrategy multiTenancyStrategy;
    protected final boolean shouldApplySchemaMigration;

    public FastBootEntityManagerFactoryBuilder(
            QuarkusPersistenceUnitDescriptor puDescriptor,
            PrevalidatedQuarkusMetadata metadata,
            StandardServiceRegistry standardServiceRegistry, RuntimeSettings runtimeSettings, Object validatorFactory,
            Object cdiBeanManager, MultiTenancyStrategy multiTenancyStrategy, boolean shouldApplySchemaMigration,
            BuiltinFormatMapperBehaviour builtinFormatMapperBehaviour) {
        this.puDescriptor = puDescriptor;
        this.metadata = metadata;
        this.standardServiceRegistry = standardServiceRegistry;
        this.runtimeSettings = runtimeSettings;
        this.validatorFactory = validatorFactory;
        this.cdiBeanManager = cdiBeanManager;
        this.multiTenancyStrategy = multiTenancyStrategy;
        this.shouldApplySchemaMigration = shouldApplySchemaMigration;
        this.builtinFormatMapperBehaviour = builtinFormatMapperBehaviour;
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
            populate(puDescriptor.getName(), optionsBuilder, standardServiceRegistry);
            return new SessionFactoryImpl(metadata, optionsBuilder.buildOptions(),
                    metadata.getTypeConfiguration().getMetadataBuildingContext().getBootstrapContext());
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
        throw new UnsupportedOperationException(
                "This isn't used for schema generation - see SessionFactoryObserverForSchemaExport instead");
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
        return "[PersistenceUnit: " + puDescriptor.getName() + "] ";
    }

    protected void populate(String persistenceUnitName, SessionFactoryOptionsBuilder options, StandardServiceRegistry ssr) {
        // will use user override value or default to false if not supplied to follow
        // JPA spec.
        final boolean jtaTransactionAccessEnabled = runtimeSettings.getBoolean(
                org.hibernate.cfg.AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS);
        if (!jtaTransactionAccessEnabled) {
            options.disableJtaTransactionAccess();
        }

        //Check for use of deprecated org.hibernate.jpa.AvailableSettings.SESSION_FACTORY_OBSERVER
        final Object legacyObserver = runtimeSettings.get("hibernate.ejb.session_factory_observer");
        if (legacyObserver != null) {
            throw new HibernateException("Legacy setting being used: 'hibernate.ejb.session_factory_observer' was replaced by '"
                    + org.hibernate.cfg.AvailableSettings.SESSION_FACTORY_OBSERVER + "'. Please update your configuration.");
        }

        // Locate and apply any requested SessionFactoryObserver
        final Object sessionFactoryObserverSetting = runtimeSettings
                .get(org.hibernate.cfg.AvailableSettings.SESSION_FACTORY_OBSERVER);
        if (sessionFactoryObserverSetting != null) {

            final StrategySelector strategySelector = ssr.getService(StrategySelector.class);
            final SessionFactoryObserver suppliedSessionFactoryObserver = strategySelector
                    .resolveStrategy(SessionFactoryObserver.class, sessionFactoryObserverSetting);
            options.addSessionFactoryObservers(suppliedSessionFactoryObserver);
        }

        options.addSessionFactoryObservers(new ServiceRegistryCloser());

        //New in ORM 6.2:
        options.addSessionFactoryObservers(new SessionFactoryObserverForNamedQueryValidation(metadata));

        // We should avoid running schema migrations multiple times
        if (shouldApplySchemaMigration) {
            options.addSessionFactoryObservers(new SessionFactoryObserverForSchemaExport(metadata));
        }
        //Vanilla ORM registers this one as well; we don't:
        //options.addSessionFactoryObservers( new SessionFactoryObserverForRegistration() );

        // This one is specific to Quarkus
        options.addSessionFactoryObservers(new QuarkusSessionFactoryObserverForDbVersionCheck());

        options.applyEntityNotFoundDelegate(new JpaEntityNotFoundDelegate());

        // This is necessary for Hibernate Reactive, see https://github.com/quarkusio/quarkus/issues/15814
        // This is also necessary for Hibernate ORM if we want to prevent calls to getters on initialized entities
        // outside of sessions from throwing exceptions, see https://github.com/quarkusio/quarkus/discussions/27657
        options.enableCollectionInDefaultFetchGroup(true);

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

        // Should be added in case of discriminator strategy too, that is not handled by options.isMultiTenancyEnabled()
        if (options.isMultiTenancyEnabled()
                || (multiTenancyStrategy != null && multiTenancyStrategy != MultiTenancyStrategy.NONE)) {
            options.applyCurrentTenantIdentifierResolver(new HibernateCurrentTenantIdentifierResolver(persistenceUnitName));
        }

        InjectableInstance<Interceptor> interceptorInstance = PersistenceUnitUtil.singleExtensionInstanceForPersistenceUnit(
                Interceptor.class, persistenceUnitName);
        if (!interceptorInstance.isUnsatisfied()) {
            options.applyStatelessInterceptorSupplier(interceptorInstance::get);
        }

        InjectableInstance<StatementInspector> statementInspectorInstance = PersistenceUnitUtil
                .singleExtensionInstanceForPersistenceUnit(StatementInspector.class, persistenceUnitName);
        if (!statementInspectorInstance.isUnsatisfied()) {
            options.applyStatementInspector(statementInspectorInstance.get());
        }

        InjectableInstance<FormatMapper> jsonFormatMapper = PersistenceUnitUtil.singleExtensionInstanceForPersistenceUnit(
                FormatMapper.class, persistenceUnitName, JsonFormat.Literal.INSTANCE);
        if (!jsonFormatMapper.isUnsatisfied()) {
            options.applyJsonFormatMapper(jsonFormatMapper.get());
        } else {
            builtinFormatMapperBehaviour.jsonApply(metadata(), persistenceUnitName);
        }
        InjectableInstance<FormatMapper> xmlFormatMapper = PersistenceUnitUtil.singleExtensionInstanceForPersistenceUnit(
                FormatMapper.class, persistenceUnitName, XmlFormat.Literal.INSTANCE);
        if (!xmlFormatMapper.isUnsatisfied()) {
            options.applyXmlFormatMapper(xmlFormatMapper.get());
        } else {
            builtinFormatMapperBehaviour.xmlApply(metadata(), persistenceUnitName);
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

        public void handleEntityNotFound(String entityName, Object id) {
            throw new EntityNotFoundException("Unable to find " + entityName + " with id " + id);
        }
    }

    @Override
    public ManagedResources getManagedResources() {
        throw new IllegalStateException("This method is not available at runtime in Quarkus");
    }

    @Override
    public MetadataImplementor metadata() {
        return metadata;
    }

}
