package org.hibernate.protean.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.ProviderChecker;

import org.jboss.logging.Logger;

final class FastbootHibernateProvider extends HibernatePersistenceProvider implements PersistenceProvider  {

	private static final Logger log = Logger.getLogger( HibernatePersistenceProvider.class );

	@Override
	public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
		return super.createEntityManagerFactory( emName, map );
	}

	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
		return super.createContainerEntityManagerFactory( info, map );
	}

	@Override
	public void generateSchema(PersistenceUnitInfo info, Map map) {
		super.generateSchema( info, map );
	}

	@Override
	public boolean generateSchema(String persistenceUnitName, Map map) {
		return super.generateSchema( persistenceUnitName, map );
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return super.getProviderUtil();
	}

	@Override
	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map properties) {
		return getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties, null, null );
	}


	/**
	 * Copied and modified from super{@link #getEntityManagerFactoryBuilderOrNull(String, Map, ClassLoader, ClassLoaderService)}
	 * Notable changes:
	 *  - ignore the ClassLoaderService and inject our own
	 *  - verify the Map properties are not set (or fail)
	 *  - don't try looking for ParsedPersistenceXmlDescriptor resources to parse, just take the pre-parsed ones in the static final field
	 */
	private EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map properties,
																			 ClassLoader providedClassLoader, ClassLoaderService providedClassLoaderService) {
		log.tracef( "Attempting to obtain correct EntityManagerFactoryBuilder for persistenceUnitName : %s", persistenceUnitName );

		verifyProperties( properties );
		Map integration = Collections.emptyMap();

		//These are pre-parsed during image generation:
		final List<ParsedPersistenceXmlDescriptor> units = PersistenceUnitsHolder.units;

		log.debugf( "Located and parsed %s persistence units; checking each", units.size() );

		if ( persistenceUnitName == null && units.size() > 1 ) {
			// no persistence-unit name to look for was given and we found multiple persistence-units
			throw new PersistenceException( "No name provided and multiple persistence units found" );
		}

		for ( ParsedPersistenceXmlDescriptor persistenceUnit : units ) {
			log.debugf(
					"Checking persistence-unit [name=%s, explicit-provider=%s] against incoming persistence unit name [%s]",
					persistenceUnit.getName(),
					persistenceUnit.getProviderClassName(),
					persistenceUnitName
			);

			final boolean matches = persistenceUnitName == null || persistenceUnit.getName().equals( persistenceUnitName );
			if ( !matches ) {
				log.debug( "Excluding from consideration due to name mis-match" );
				continue;
			}

			// See if we (Hibernate) are the persistence provider
			if ( ! ProviderChecker.isProvider( persistenceUnit, properties ) ) {
				log.debug( "Excluding from consideration due to provider mis-match" );
				continue;
			}

			ClassLoaderService overrideClassLoaderService = new FlatClassLoaderService();
			return getEntityManagerFactoryBuilder( persistenceUnit, integration, overrideClassLoaderService );
		}

		log.debug( "Found no matching persistence units" );
		return null;
	}

	private void verifyProperties(Map properties) {
		if ( properties != null && properties.size() != 0 ) {
			throw new PersistenceException( "The FastbootHibernateProvider PersistenceProvider can not support runtime provided properties. " +
													"Make sure you set all properties you need in the configuration resources before building the application." );
		}
	}
}
