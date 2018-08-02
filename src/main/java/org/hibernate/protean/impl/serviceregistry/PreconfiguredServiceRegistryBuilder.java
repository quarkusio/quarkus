package org.hibernate.protean.impl.serviceregistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.cfgxml.internal.CfgXmlAccessServiceInitiator;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cache.internal.RegionFactoryInitiator;
import org.hibernate.engine.config.internal.ConfigurationServiceInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.engine.jdbc.cursor.internal.RefCursorSupportInitiator;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryInitiator;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.jndi.internal.JndiServiceInitiator;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformResolverInitiator;
import org.hibernate.hql.internal.QueryTranslatorFactoryInitiator;
import org.hibernate.id.factory.internal.MutableIdentifierGeneratorFactoryInitiator;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jmx.internal.JmxServiceInitiator;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.internal.PersisterFactoryInitiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyResolverInitiator;
import org.hibernate.protean.impl.FlatClassLoaderService;
import org.hibernate.resource.beans.spi.ManagedBeanRegistryInitiator;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.service.Service;
import org.hibernate.service.StandardServiceInitiators;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractorInitiator;
import org.hibernate.tool.schema.internal.SchemaManagementToolInitiator;

import static org.hibernate.internal.HEMLogging.messageLogger;

/**
 * Helps to instantiate a ServiceRegistryBuilder from a previous state.
 * This will perform only minimal configuration validation and will never modify the given configuration properties.
 * <p>
 * Meant to be used only to rebuild a previously created ServiceRegistry, which has been created via the traditional
 * methods, so this builder expects much more explicit input.
 */
public class PreconfiguredServiceRegistryBuilder {

	private static final EntityManagerMessageLogger LOG = messageLogger( PreconfiguredServiceRegistryBuilder.class );

	private final Map configurationValues = new HashMap();
	private final List<StandardServiceInitiator> initiators = standardInitiatorList();
	private final List<ProvidedService> providedServices = new ArrayList<ProvidedService>();
	private final MirroringIntegratorService integrators = new MirroringIntegratorService();

	public PreconfiguredServiceRegistryBuilder() {
	}

	public PreconfiguredServiceRegistryBuilder applySetting(String settingName, Object value) {
		configurationValues.put( settingName, value );
		return this;
	}

	public PreconfiguredServiceRegistryBuilder applyIntegrator(Integrator integrator) {
		integrators.addIntegrator( integrator );
		return this;
	}

	public PreconfiguredServiceRegistryBuilder addInitiator(StandardServiceInitiator initiator) {
		initiators.add( initiator );
		return this;
	}

	public PreconfiguredServiceRegistryBuilder addService(final Class serviceRole, final Service service) {
		providedServices.add( new ProvidedService( serviceRole, service ) );
		return this;
	}

	public StandardServiceRegistry buildNewServiceRegistry() {
		final BootstrapServiceRegistry bootstrapServiceRegistry = buildBootstrapServiceRegistry();

		//Can skip, it's only deprecated stuff:
		//applyServiceContributingIntegrators( bootstrapServiceRegistry );

		//This is NOT deprecated stuff.. yet they will at best contribute stuff we already recorded as part of #applyIntegrator, #addInitiator, #addService
		//applyServiceContributors( bootstrapServiceRegistry );

		final Map settingsCopy = new HashMap();
		settingsCopy.putAll( configurationValues );
		ConfigurationHelper.resolvePlaceHolders( settingsCopy );

		return new StandardServiceRegistryImpl(
				true,
				bootstrapServiceRegistry,
				initiators,
				providedServices,
				settingsCopy
		);
	}

	private BootstrapServiceRegistry buildBootstrapServiceRegistry() {

		//N.B. support for custom IntegratorProvider injected via Properties (as instance) removed

		//N.B. support for custom StrategySelector is not implemented yet: see MirroringStrategySelector

		return new BootstrapServiceRegistryImpl(
				true,
				FlatClassLoaderService.INSTANCE,
				new MirroringStrategySelector(),
				integrators
		);
	}

	private static List<StandardServiceInitiator> standardInitiatorList() {
		//Override initiatior List? Some need to be replaced, e.g. ConnectionProviderInitiator, DialectResolverInitiator,
		final List<StandardServiceInitiator> initiators = new ArrayList<StandardServiceInitiator>(
				StandardServiceInitiators.LIST.size() );
		initiators.addAll( StandardServiceInitiators.LIST );
		return initiators;
	}


	/**
	 * Modified copy from org.hibernate.service.StandardServiceInitiators#buildStandardServiceInitiatorList
	 *
	 * N.B. not to be confused with org.hibernate.service.internal.StandardSessionFactoryServiceInitiators#buildStandardServiceInitiatorList()
	 * @return
	 */
	private static List<StandardServiceInitiator> buildStandardServiceInitiatorList() {
		final ArrayList<StandardServiceInitiator> serviceInitiators = new ArrayList<StandardServiceInitiator>();

		//Harmless yet pointless
		serviceInitiators.add( CfgXmlAccessServiceInitiator.INSTANCE );
		serviceInitiators.add( ConfigurationServiceInitiator.INSTANCE );
		serviceInitiators.add( PropertyAccessStrategyResolverInitiator.INSTANCE );

		serviceInitiators.add( ImportSqlCommandExtractorInitiator.INSTANCE );
		serviceInitiators.add( SchemaManagementToolInitiator.INSTANCE );

		serviceInitiators.add( JdbcEnvironmentInitiator.INSTANCE );
		serviceInitiators.add( JndiServiceInitiator.INSTANCE );
		serviceInitiators.add( JmxServiceInitiator.INSTANCE );

		serviceInitiators.add( PersisterClassResolverInitiator.INSTANCE );
		serviceInitiators.add( PersisterFactoryInitiator.INSTANCE );

		serviceInitiators.add( ConnectionProviderInitiator.INSTANCE );
		serviceInitiators.add( MultiTenantConnectionProviderInitiator.INSTANCE );
		serviceInitiators.add( DialectResolverInitiator.INSTANCE );
		serviceInitiators.add( DialectFactoryInitiator.INSTANCE );
		serviceInitiators.add( BatchBuilderInitiator.INSTANCE );
		serviceInitiators.add( JdbcServicesInitiator.INSTANCE );
		serviceInitiators.add( RefCursorSupportInitiator.INSTANCE );

		serviceInitiators.add( QueryTranslatorFactoryInitiator.INSTANCE );
		serviceInitiators.add( MutableIdentifierGeneratorFactoryInitiator.INSTANCE);

		serviceInitiators.add( JtaPlatformResolverInitiator.INSTANCE );
		serviceInitiators.add( JtaPlatformInitiator.INSTANCE );

		serviceInitiators.add( SessionFactoryServiceRegistryFactoryInitiator.INSTANCE );

		serviceInitiators.add( RegionFactoryInitiator.INSTANCE );

		serviceInitiators.add( TransactionCoordinatorBuilderInitiator.INSTANCE );

		serviceInitiators.add( ManagedBeanRegistryInitiator.INSTANCE );

		serviceInitiators.trimToSize();

		return Collections.unmodifiableList( serviceInitiators );
	}


}
