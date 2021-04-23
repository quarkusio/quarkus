package io.quarkus.hibernate.orm.runtime.boot.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.registry.selector.internal.StrategySelectorImpl;
import org.hibernate.engine.config.internal.ConfigurationServiceInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.engine.jdbc.cursor.internal.RefCursorSupportInitiator;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.event.internal.EntityCopyObserverFactoryInitiator;
import org.hibernate.hql.internal.QueryTranslatorFactoryInitiator;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.internal.PersisterFactoryInitiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyResolverInitiator;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.tool.schema.internal.SchemaManagementToolInitiator;

import io.quarkus.hibernate.orm.runtime.cdi.QuarkusManagedBeanRegistryInitiator;
import io.quarkus.hibernate.orm.runtime.customized.DisabledBytecodeProviderInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProviderInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusJndiServiceInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusJtaPlatformInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusRuntimeProxyFactoryFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.recording.RecordedState;
import io.quarkus.hibernate.orm.runtime.service.CfgXmlAccessServiceInitiatorQuarkus;
import io.quarkus.hibernate.orm.runtime.service.DisabledJMXInitiator;
import io.quarkus.hibernate.orm.runtime.service.FlatClassLoaderService;
import io.quarkus.hibernate.orm.runtime.service.QuarkusImportSqlCommandExtractorInitiator;
import io.quarkus.hibernate.orm.runtime.service.QuarkusRegionFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.service.QuarkusStaticDialectFactoryInitiator;

/**
 * Helps to instantiate a ServiceRegistryBuilder from a previous state. This
 * will perform only minimal configuration validation and will never modify the
 * given configuration properties.
 * <p>
 * Meant to be used only to rebuild a previously created ServiceRegistry, which
 * has been created via the traditional methods, so this builder expects much
 * more explicit input.
 */
public class PreconfiguredServiceRegistryBuilder {

    private final Map configurationValues = new HashMap();
    private final List<StandardServiceInitiator> initiators;
    private final List<ProvidedService> providedServices = new ArrayList<ProvidedService>();
    private final Collection<Integrator> integrators;
    private final StandardServiceRegistryImpl destroyedRegistry;

    public PreconfiguredServiceRegistryBuilder(RecordedState rs) {
        checkIsNotReactive(rs);
        this.initiators = buildQuarkusServiceInitiatorList(rs);
        this.integrators = rs.getIntegrators();
        this.destroyedRegistry = (StandardServiceRegistryImpl) rs.getMetadata()
                .getMetadataBuildingOptions()
                .getServiceRegistry();
    }

    private void checkIsNotReactive(RecordedState rs) {
        if (rs.isReactive())
            throw new IllegalStateException("Booting a blocking Hibernate ORM serviceregistry on a Reactive RecordedState!");
    }

    public PreconfiguredServiceRegistryBuilder applySetting(String settingName, Object value) {
        configurationValues.put(settingName, value);
        return this;
    }

    public PreconfiguredServiceRegistryBuilder addInitiator(StandardServiceInitiator initiator) {
        initiators.add(initiator);
        return this;
    }

    public PreconfiguredServiceRegistryBuilder addIntegrator(Integrator integrator) {
        integrators.add(integrator);
        return this;
    }

    public PreconfiguredServiceRegistryBuilder addService(ProvidedService providedService) {
        providedServices.add(providedService);
        return this;
    }

    public StandardServiceRegistryImpl buildNewServiceRegistry() {
        final BootstrapServiceRegistry bootstrapServiceRegistry = buildEmptyBootstrapServiceRegistry();

        // Can skip, it's only deprecated stuff:
        // applyServiceContributingIntegrators( bootstrapServiceRegistry );

        // This is NOT deprecated stuff.. yet they will at best contribute stuff we
        // already recorded as part of #applyIntegrator, #addInitiator, #addService
        // applyServiceContributors( bootstrapServiceRegistry );

        final Map settingsCopy = new HashMap();
        settingsCopy.putAll(configurationValues);

        // FIXME: resetAndReactivate() throws "IllegalStateException: Can't reactivate an active registry!"
        //  during persistenceProvider.generateSchema() execution (a new PersistenceProvider instance is constructed
        //  during this call).
        //  Is it OK to skip the resetAndReactivate() call when the registry is already active?
        if (!destroyedRegistry.isActive()) {
            destroyedRegistry.resetAndReactivate(bootstrapServiceRegistry, initiators, providedServices, settingsCopy);
        }
        return destroyedRegistry;

        //		return new StandardServiceRegistryImpl(
        //				true,
        //				bootstrapServiceRegistry,
        //				initiators,
        //				providedServices,
        //				settingsCopy
        //		);
    }

    private BootstrapServiceRegistry buildEmptyBootstrapServiceRegistry() {

        // N.B. support for custom IntegratorProvider injected via Properties (as
        // instance) removed

        // N.B. support for custom StrategySelector is not implemented yet

        final StrategySelectorImpl strategySelector = new StrategySelectorImpl(FlatClassLoaderService.INSTANCE);

        return new BootstrapServiceRegistryImpl(true,
                FlatClassLoaderService.INSTANCE,
                strategySelector, // new MirroringStrategySelector(),
                new MirroringIntegratorService(integrators));
    }

    /**
     * Modified copy from
     * org.hibernate.service.StandardServiceInitiators#buildStandardServiceInitiatorList
     * <p>
     * N.B. not to be confused with
     * org.hibernate.service.internal.StandardSessionFactoryServiceInitiators#buildStandardServiceInitiatorList()
     *
     * @return
     */
    private static List<StandardServiceInitiator> buildQuarkusServiceInitiatorList(RecordedState rs) {
        final ArrayList<StandardServiceInitiator> serviceInitiators = new ArrayList<StandardServiceInitiator>();

        //Enforces no bytecode enhancement will happen at runtime:
        serviceInitiators.add(DisabledBytecodeProviderInitiator.INSTANCE);

        //Use a custom ProxyFactoryFactory which is able to use the class definitions we already created:
        serviceInitiators.add(new QuarkusRuntimeProxyFactoryFactoryInitiator(rs));

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

        // Useful as-is: it performs detection of driver capabilities in particular
        serviceInitiators.add(JdbcEnvironmentInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusJndiServiceInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(DisabledJMXInitiator.INSTANCE);

        serviceInitiators.add(PersisterClassResolverInitiator.INSTANCE);
        serviceInitiators.add(PersisterFactoryInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusConnectionProviderInitiator.INSTANCE);
        serviceInitiators.add(MultiTenantConnectionProviderInitiator.INSTANCE);

        // Disabled: Dialect is injected explicitly
        // serviceInitiators.add( DialectResolverInitiator.INSTANCE );

        // Custom one: Dialect is injected explicitly
        serviceInitiators.add(new QuarkusStaticDialectFactoryInitiator(rs.getDialect()));

        serviceInitiators.add(BatchBuilderInitiator.INSTANCE);
        serviceInitiators.add(JdbcServicesInitiator.INSTANCE);
        serviceInitiators.add(RefCursorSupportInitiator.INSTANCE);

        serviceInitiators.add(QueryTranslatorFactoryInitiator.INSTANCE);

        // Disabled: IdentifierGenerators are no longer initiated after Metadata was generated.
        // serviceInitiators.add(MutableIdentifierGeneratorFactoryInitiator.INSTANCE);

        serviceInitiators.add(QuarkusJtaPlatformInitiator.INSTANCE);

        serviceInitiators.add(SessionFactoryServiceRegistryFactoryInitiator.INSTANCE);

        // Replaces RegionFactoryInitiator.INSTANCE
        serviceInitiators.add(QuarkusRegionFactoryInitiator.INSTANCE);

        serviceInitiators.add(TransactionCoordinatorBuilderInitiator.INSTANCE);

        // Replaces ManagedBeanRegistryInitiator.INSTANCE
        serviceInitiators.add(QuarkusManagedBeanRegistryInitiator.INSTANCE);

        serviceInitiators.add(EntityCopyObserverFactoryInitiator.INSTANCE);

        serviceInitiators.trimToSize();
        return serviceInitiators;
    }

}
