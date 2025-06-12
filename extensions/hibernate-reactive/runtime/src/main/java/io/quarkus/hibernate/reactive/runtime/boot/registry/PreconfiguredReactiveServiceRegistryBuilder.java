package io.quarkus.hibernate.reactive.runtime.boot.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.engine.config.internal.ConfigurationServiceInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.engine.jdbc.cursor.internal.RefCursorSupportInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.jdbc.internal.SqlStatementLoggerInitiator;
import org.hibernate.event.internal.EntityCopyObserverFactoryInitiator;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.persister.internal.PersisterFactoryInitiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyResolverInitiator;
import org.hibernate.reactive.engine.jdbc.mutation.internal.ReactiveMutationExecutorServiceInitiator;
import org.hibernate.reactive.loader.ast.internal.ReactiveBatchLoaderFactoryInitiator;
import org.hibernate.reactive.provider.service.NativeParametersHandling;
import org.hibernate.reactive.provider.service.NoJdbcEnvironmentInitiator;
import org.hibernate.reactive.provider.service.NoJtaPlatformInitiator;
import org.hibernate.reactive.provider.service.ReactiveMarkerServiceInitiator;
import org.hibernate.reactive.provider.service.ReactivePersisterClassResolverInitiator;
import org.hibernate.reactive.provider.service.ReactiveSchemaManagementToolInitiator;
import org.hibernate.reactive.provider.service.ReactiveSessionFactoryBuilderInitiator;
import org.hibernate.reactive.provider.service.ReactiveSqmMultiTableMutationStrategyProviderInitiator;
import org.hibernate.reactive.provider.service.ReactiveValuesMappingProducerProviderInitiator;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.tool.schema.internal.SchemaManagementToolInitiator;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfigPersistenceUnit;
import io.quarkus.hibernate.orm.runtime.boot.registry.MirroringIntegratorService;
import io.quarkus.hibernate.orm.runtime.cdi.QuarkusManagedBeanRegistryInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusJndiServiceInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusRuntimeProxyFactoryFactory;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusRuntimeProxyFactoryFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusStrategySelectorBuilder;
import io.quarkus.hibernate.orm.runtime.recording.RecordedState;
import io.quarkus.hibernate.orm.runtime.service.CfgXmlAccessServiceInitiatorQuarkus;
import io.quarkus.hibernate.orm.runtime.service.FlatClassLoaderService;
import io.quarkus.hibernate.orm.runtime.service.QuarkusImportSqlCommandExtractorInitiator;
import io.quarkus.hibernate.orm.runtime.service.QuarkusRegionFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.service.QuarkusRuntimeInitDialectFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.service.QuarkusRuntimeInitDialectResolverInitiator;
import io.quarkus.hibernate.orm.runtime.service.bytecodeprovider.QuarkusRuntimeBytecodeProviderInitiator;
import io.quarkus.hibernate.reactive.runtime.customized.CheckingVertxContextInitiator;
import io.quarkus.hibernate.reactive.runtime.customized.QuarkusNoJdbcConnectionProviderInitiator;

/**
 * Helps to instantiate a ServiceRegistryBuilder from a previous state. This
 * will perform only minimal configuration validation and will never modify the
 * given configuration properties.
 * <p>
 * Meant to be used only to rebuild a previously created ServiceRegistry, which
 * has been created via the traditional methods, so this builder expects much
 * more explicit input.
 */
public class PreconfiguredReactiveServiceRegistryBuilder {

    private final Map configurationValues = new HashMap();
    private final List<StandardServiceInitiator<?>> initiators;
    private final List<ProvidedService<?>> providedServices = new ArrayList<>();
    private final Collection<Integrator> integrators;
    private final StandardServiceRegistryImpl destroyedRegistry;

    public PreconfiguredReactiveServiceRegistryBuilder(String puConfigName, RecordedState rs,
            HibernateOrmRuntimeConfigPersistenceUnit puConfig) {
        checkIsReactive(rs);
        this.initiators = buildQuarkusServiceInitiatorList(puConfigName, rs, puConfig);
        this.integrators = rs.getIntegrators();
        this.destroyedRegistry = (StandardServiceRegistryImpl) rs.getMetadata()
                .getMetadataBuildingOptions()
                .getServiceRegistry();
    }

    private static void checkIsReactive(RecordedState rs) {
        if (rs.isReactive() == false)
            throw new IllegalStateException("Booting an Hibernate Reactive serviceregistry on a non-reactive RecordedState!");
    }

    public PreconfiguredReactiveServiceRegistryBuilder applySetting(String settingName, Object value) {
        configurationValues.put(settingName, value);
        return this;
    }

    public PreconfiguredReactiveServiceRegistryBuilder addInitiator(StandardServiceInitiator initiator) {
        initiators.add(initiator);
        return this;
    }

    public PreconfiguredReactiveServiceRegistryBuilder addService(ProvidedService providedService) {
        providedServices.add(providedService);
        return this;
    }

    public StandardServiceRegistryImpl buildNewServiceRegistry() {
        final BootstrapServiceRegistry bootstrapServiceRegistry = buildEmptyBootstrapServiceRegistry();

        // Can skip, it's only deprecated stuff:
        // applyServiceContributingIntegrators( bootstrapServiceRegistry );

        // This is NOT deprecated stuff, yet they will at best contribute stuff we
        // already recorded as part of #applyIntegrator, #addInitiator, #addService
        // applyServiceContributors( bootstrapServiceRegistry );

        final Map settingsCopy = new HashMap();
        settingsCopy.putAll(configurationValues);

        destroyedRegistry.resetAndReactivate(bootstrapServiceRegistry, initiators, providedServices, settingsCopy);
        return destroyedRegistry;
    }

    private BootstrapServiceRegistry buildEmptyBootstrapServiceRegistry() {
        final ClassLoaderService providedClassLoaderService = FlatClassLoaderService.INSTANCE;

        // N.B. support for custom IntegratorProvider injected via Properties (as
        // instance) removed

        // N.B. support for custom StrategySelector is not implemented yet

        // A non-empty selector is needed in order to support ID generators that retrieve a naming strategy -- at runtime!
        var strategySelector = QuarkusStrategySelectorBuilder.buildRuntimeSelector(providedClassLoaderService);

        return new BootstrapServiceRegistryImpl(true,
                providedClassLoaderService,
                strategySelector, // new MirroringStrategySelector(),
                new MirroringIntegratorService(integrators));
    }

    /**
     * Modified copy from
     * org.hibernate.service.StandardServiceInitiators#buildStandardServiceInitiatorList
     *
     * N.B. not to be confused with
     * org.hibernate.service.internal.StandardSessionFactoryServiceInitiators#buildStandardServiceInitiatorList()
     *
     * @return
     */
    private static List<StandardServiceInitiator<?>> buildQuarkusServiceInitiatorList(String puConfigName, RecordedState rs,
            HibernateOrmRuntimeConfigPersistenceUnit puConfig) {
        final ArrayList<StandardServiceInitiator<?>> serviceInitiators = new ArrayList<>();

        //References to this object need to be injected in both the initiator for BytecodeProvider and for
        //the registered ProxyFactoryFactoryInitiator
        QuarkusRuntimeProxyFactoryFactory statefulProxyFactory = new QuarkusRuntimeProxyFactoryFactory(
                rs.getProxyClassDefinitions());

        // Definitely exclusive to Hibernate Reactive, as it marks the registry as Reactive:
        serviceInitiators.add(ReactiveMarkerServiceInitiator.INSTANCE);

        // Custom to Quarkus: Hibernate Reactive upstream would use org.hibernate.reactive.context.impl.VertxContextInitiator
        serviceInitiators.add(CheckingVertxContextInitiator.INSTANCE);

        //Custom for Hibernate Reactive:
        serviceInitiators.add(ReactiveSessionFactoryBuilderInitiator.INSTANCE);

        //Enforces no bytecode enhancement will happen at runtime,
        //but allows use of proxies generated at build time
        serviceInitiators.add(new QuarkusRuntimeBytecodeProviderInitiator(statefulProxyFactory));

        //Use a custom ProxyFactoryFactory which is able to use the class definitions we already created:
        serviceInitiators.add(new QuarkusRuntimeProxyFactoryFactoryInitiator(statefulProxyFactory));

        serviceInitiators.add(ReactiveMutationExecutorServiceInitiator.INSTANCE);

        // Replaces org.hibernate.boot.cfgxml.internal.CfgXmlAccessServiceInitiator :
        // not used
        // (Original disabled)
        serviceInitiators.add(CfgXmlAccessServiceInitiatorQuarkus.INSTANCE);

        // Useful as-is
        serviceInitiators.add(ConfigurationServiceInitiator.INSTANCE);

        // TODO (optional): assume entities are already enhanced?
        serviceInitiators.add(PropertyAccessStrategyResolverInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusImportSqlCommandExtractorInitiator.INSTANCE);

        // TODO disable?
        serviceInitiators.add(SchemaManagementToolInitiator.INSTANCE);

        serviceInitiators.add(NoJdbcEnvironmentInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusJndiServiceInitiator.INSTANCE);

        //Custom for Hibernate Reactive:
        serviceInitiators.add(ReactivePersisterClassResolverInitiator.INSTANCE);
        serviceInitiators.add(PersisterFactoryInitiator.INSTANCE);

        //Custom for Hibernate Reactive:
        serviceInitiators.add(QuarkusNoJdbcConnectionProviderInitiator.INSTANCE);
        serviceInitiators.add(MultiTenantConnectionProviderInitiator.INSTANCE);

        // Custom one: Dialect is injected explicitly
        serviceInitiators.add(new QuarkusRuntimeInitDialectResolverInitiator(rs.getDialect()));

        // Custom one: Dialect is injected explicitly
        serviceInitiators.add(new QuarkusRuntimeInitDialectFactoryInitiator(puConfigName, rs.isFromPersistenceXml(),
                rs.getDialect(), rs.getBuildTimeSettings().getSource(), puConfig));

        // Default implementation
        serviceInitiators.add(BatchBuilderInitiator.INSTANCE);
        serviceInitiators.add(JdbcServicesInitiator.INSTANCE);
        serviceInitiators.add(RefCursorSupportInitiator.INSTANCE);

        // Custom for Hibernate Reactive:
        serviceInitiators.add(ReactiveSchemaManagementToolInitiator.INSTANCE);

        // Disabled: IdentifierGenerators are no longer initiated after Metadata was generated.
        // serviceInitiators.add(MutableIdentifierGeneratorFactoryInitiator.INSTANCE);

        // Custom for Hibernate Reactive:
        serviceInitiators.add(NoJtaPlatformInitiator.INSTANCE);

        serviceInitiators.add(SessionFactoryServiceRegistryFactoryInitiator.INSTANCE);

        // Replaces RegionFactoryInitiator.INSTANCE
        serviceInitiators.add(QuarkusRegionFactoryInitiator.INSTANCE);

        serviceInitiators.add(TransactionCoordinatorBuilderInitiator.INSTANCE);

        // Replaces ManagedBeanRegistryInitiator.INSTANCE
        serviceInitiators.add(QuarkusManagedBeanRegistryInitiator.INSTANCE);

        serviceInitiators.add(EntityCopyObserverFactoryInitiator.INSTANCE);

        //Custom for Hibernate Reactive:
        serviceInitiators.add(ReactiveValuesMappingProducerProviderInitiator.INSTANCE);

        //Custom for Hibernate Reactive:
        serviceInitiators.add(ReactiveSqmMultiTableMutationStrategyProviderInitiator.INSTANCE);

        // Custom for Hibernate Reactive: ParameterMarkerStrategy
        serviceInitiators.add(NativeParametersHandling.INSTANCE);

        // Default implementation
        serviceInitiators.add(SqlStatementLoggerInitiator.INSTANCE);

        // Custom for Hibernate Reactive: BatchLoaderFactory
        serviceInitiators.add(ReactiveBatchLoaderFactoryInitiator.INSTANCE);

        serviceInitiators.trimToSize();
        return serviceInitiators;
    }

}
