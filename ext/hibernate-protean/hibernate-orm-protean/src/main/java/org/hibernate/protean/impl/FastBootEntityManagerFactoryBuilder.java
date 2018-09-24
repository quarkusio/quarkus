package org.hibernate.protean.impl;

import java.io.Serializable;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

final class FastBootEntityManagerFactoryBuilder implements EntityManagerFactoryBuilder {

	private final MetadataImplementor metadata;
	private final String persistenceUnitName;
	private final StandardServiceRegistry standardServiceRegistry;
	private final Map configurationValues;
	private final Object validatorFactory;
	private final Object cdiBeanManager;

	public FastBootEntityManagerFactoryBuilder(
			MetadataImplementor metadata,
			String persistenceUnitName,
			StandardServiceRegistry standardServiceRegistry,
			Map configurationValues,
			Object validatorFactory,
			Object cdiBeanManager) {
		this.metadata = metadata;
		this.persistenceUnitName = persistenceUnitName;
		this.standardServiceRegistry = standardServiceRegistry;
		this.configurationValues = configurationValues;
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
		SessionFactoryBuilder sfBuilder = metadata.getSessionFactoryBuilder();
		populate( sfBuilder, standardServiceRegistry );
		try {
			return sfBuilder.build();
		}
		catch (Exception e) {
			throw persistenceException( "Unable to build Hibernate SessionFactory", e );
		}
	}

	@Override
	public void cancel() {
		// nothing?
	}

	@Override
	public void generateSchema() {
		// This seems overkill, but building the SF is necessary to get the Integrators to kick in.
		// Metamodel will clean this up...
		try {
			SessionFactoryBuilder sfBuilder = metadata.getSessionFactoryBuilder();
			populate( sfBuilder, standardServiceRegistry );

			SchemaManagementToolCoordinator.process(
					metadata, standardServiceRegistry, configurationValues, DelayedDropRegistryNotAvailableImpl.INSTANCE
			);
		}
		catch (Exception e) {
			throw persistenceException( "Error performing schema management", e );
		}

		// release this builder
		cancel();
	}

	private PersistenceException persistenceException(String message, Exception cause) {
		return new PersistenceException(
				getExceptionHeader() + message,
				cause
		);
	}

	private String getExceptionHeader() {
		return "[PersistenceUnit: " + persistenceUnitName + "] ";
	}


	protected void populate(SessionFactoryBuilder sfBuilder, StandardServiceRegistry ssr) {

		// will use user override value or default to false if not supplied to follow JPA spec.
		final boolean jtaTransactionAccessEnabled = readBooleanConfigurationValue( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS );
		if ( !jtaTransactionAccessEnabled ) {
			( (SessionFactoryBuilderImplementor) sfBuilder ).disableJtaTransactionAccess();
		}

		final boolean allowRefreshDetachedEntity = readBooleanConfigurationValue( org.hibernate.cfg.AvailableSettings.ALLOW_REFRESH_DETACHED_ENTITY );
		if ( !allowRefreshDetachedEntity ) {
			( (SessionFactoryBuilderImplementor) sfBuilder ).disableRefreshDetachedEntity();
		}

		// Locate and apply any requested SessionFactoryObserver
		final Object sessionFactoryObserverSetting = configurationValues.remove( AvailableSettings.SESSION_FACTORY_OBSERVER );
		if ( sessionFactoryObserverSetting != null ) {

			final StrategySelector strategySelector = ssr.getService( StrategySelector.class );
			final SessionFactoryObserver suppliedSessionFactoryObserver =
					strategySelector.resolveStrategy( SessionFactoryObserver.class, sessionFactoryObserverSetting );
			sfBuilder.addSessionFactoryObservers( suppliedSessionFactoryObserver );
		}

		sfBuilder.addSessionFactoryObservers( new ServiceRegistryCloser() );

		sfBuilder.applyEntityNotFoundDelegate( new JpaEntityNotFoundDelegate() );

		if ( this.validatorFactory != null ) {
			sfBuilder.applyValidatorFactory( validatorFactory );
		}
		if ( this.cdiBeanManager != null ) {
			sfBuilder.applyBeanManager( cdiBeanManager );
		}
	}

	private boolean readBooleanConfigurationValue(String propertyName) {
		Object propertyValue = configurationValues.remove( propertyName );
		return propertyValue != null && Boolean.parseBoolean( propertyValue.toString() );
	}

	private static class ServiceRegistryCloser implements SessionFactoryObserver {

		@Override
		public void sessionFactoryCreated(SessionFactory sessionFactory) {
			// nothing to do
		}

		@Override
		public void sessionFactoryClosed(SessionFactory sessionFactory) {
			SessionFactoryImplementor sfi = ( (SessionFactoryImplementor) sessionFactory );
			sfi.getServiceRegistry().destroy();
			ServiceRegistry basicRegistry = sfi.getServiceRegistry().getParentServiceRegistry();
			( (ServiceRegistryImplementor) basicRegistry ).destroy();
		}
	}

	private static class JpaEntityNotFoundDelegate implements EntityNotFoundDelegate, Serializable {

		public void handleEntityNotFound(String entityName, Serializable id) {
			throw new EntityNotFoundException( "Unable to find " + entityName  + " with id " + id );
		}
	}

}
