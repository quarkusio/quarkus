package org.hibernate.protean.impl;

import java.util.Map;
import java.util.Objects;

import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;

import static org.hibernate.cfg.AvailableSettings.DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.JPA_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JPA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_TYPE;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;
import static org.hibernate.internal.HEMLogging.messageLogger;

public class ServiceRegistryBuilder {

	private static final EntityManagerMessageLogger LOG = messageLogger( ServiceRegistryBuilder.class );

	private final PersistenceUnitDescriptor persistenceUnit;
	private final Map configurationValues;

	public ServiceRegistryBuilder(PersistenceUnitDescriptor persistenceUnit, Map configurationValues) {
		this.persistenceUnit = Objects.requireNonNull( persistenceUnit );
		this.configurationValues = Objects.requireNonNull( configurationValues );
	}

	StandardServiceRegistry buildNewServiceRegistry() {
		final ClassLoaderService providedClassLoaderService = FlatClassLoaderService.INSTANCE;
		final BootstrapServiceRegistry bsr = buildBootstrapServiceRegistry( providedClassLoaderService );

		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );

		ssrBuilder.applySettings( configurationValues );
		configure( ssrBuilder );
		return ssrBuilder.build();
	}

	private BootstrapServiceRegistry buildBootstrapServiceRegistry(ClassLoaderService providedClassLoaderService) {

		final BootstrapServiceRegistryBuilder bsrBuilder = new BootstrapServiceRegistryBuilder();

		//N.B. support for custom IntegratorProvider injected via Properties (as instance) removed

		//N.B. support for custom StrategySelector removed
		//TODO see to inject a custom org.hibernate.boot.registry.selector.spi.StrategySelector ?

		bsrBuilder.applyClassLoaderService( providedClassLoaderService );

		return bsrBuilder.build();
	}

	private void configure(StandardServiceRegistryBuilder ssrBuilder) {
		applyJdbcConnectionProperties( ssrBuilder );
		applyTransactionProperties( ssrBuilder );
		// flush before completion validation
		if ( "true".equals( configurationValues.get( Environment.FLUSH_BEFORE_COMPLETION ) ) ) {
			ssrBuilder.applySetting( Environment.FLUSH_BEFORE_COMPLETION, "false" );
			LOG.definingFlushBeforeCompletionIgnoredInHem( Environment.FLUSH_BEFORE_COMPLETION );
		}
	}
	private void applyJdbcConnectionProperties(StandardServiceRegistryBuilder ssrBuilder) {
		if ( persistenceUnit.getJtaDataSource() != null ) {
			if ( ! ssrBuilder.getSettings().containsKey( DATASOURCE ) ) {
				ssrBuilder.applySetting( DATASOURCE, persistenceUnit.getJtaDataSource() );
				// HHH-8121 : make the PU-defined value available to EMF.getProperties()
				configurationValues.put( JPA_JTA_DATASOURCE, persistenceUnit.getJtaDataSource() );
			}
		}
		else if ( persistenceUnit.getNonJtaDataSource() != null ) {
			if ( ! ssrBuilder.getSettings().containsKey( DATASOURCE ) ) {
				ssrBuilder.applySetting( DATASOURCE, persistenceUnit.getNonJtaDataSource() );
				// HHH-8121 : make the PU-defined value available to EMF.getProperties()
				configurationValues.put( JPA_NON_JTA_DATASOURCE, persistenceUnit.getNonJtaDataSource() );
			}
		}
		else {
			final String driver = (String) configurationValues.get( JPA_JDBC_DRIVER );
			if ( StringHelper.isNotEmpty( driver ) ) {
				ssrBuilder.applySetting( DRIVER, driver );
			}
			final String url = (String) configurationValues.get( JPA_JDBC_URL );
			if ( StringHelper.isNotEmpty( url ) ) {
				ssrBuilder.applySetting( URL, url );
			}
			final String user = (String) configurationValues.get( JPA_JDBC_USER );
			if ( StringHelper.isNotEmpty( user ) ) {
				ssrBuilder.applySetting( USER, user );
			}
			final String pass = (String) configurationValues.get( JPA_JDBC_PASSWORD );
			if ( StringHelper.isNotEmpty( pass ) ) {
				ssrBuilder.applySetting( PASS, pass );
			}
		}
	}

	private void applyTransactionProperties(StandardServiceRegistryBuilder ssrBuilder) {
		PersistenceUnitTransactionType txnType = PersistenceUnitTransactionTypeHelper.interpretTransactionType(
				configurationValues.get( JPA_TRANSACTION_TYPE )
		);
		if ( txnType == null ) {
			txnType = persistenceUnit.getTransactionType();
		}
		if ( txnType == null ) {
			// is it more appropriate to have this be based on bootstrap entry point (EE vs SE)?
			txnType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		boolean hasTxStrategy = configurationValues.containsKey( TRANSACTION_COORDINATOR_STRATEGY );
		if ( hasTxStrategy ) {
			LOG.overridingTransactionStrategyDangerous( TRANSACTION_COORDINATOR_STRATEGY );
		}
		else {
			if ( txnType == PersistenceUnitTransactionType.JTA ) {
				ssrBuilder.applySetting( TRANSACTION_COORDINATOR_STRATEGY, JtaTransactionCoordinatorBuilderImpl.class );
			}
			else if ( txnType == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
				ssrBuilder.applySetting( TRANSACTION_COORDINATOR_STRATEGY, JdbcResourceLocalTransactionCoordinatorBuilderImpl.class );
			}
		}
	}

}
