package org.hibernate.protean.impl;

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

final class FastbootHibernateProvider extends HibernatePersistenceProvider implements PersistenceProvider  {

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


	private EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map properties,
																			 ClassLoader providedClassLoader, ClassLoaderService providedClassLoaderService) {
		System.out.println( "Attempting to obtain EntityManagerFactoryBuilder for persistenceUnitName : "+ persistenceUnitName );

		final Map integration = wrap( properties );
		final List<ParsedPersistenceXmlDescriptor> units;
		try {
			units = PersistenceXmlParser.locatePersistenceUnits( integration );
		}
		catch (Exception e) {
			System.out.println( "Unable to locate persistence units" );
			throw new PersistenceException( "Unable to locate persistence units", e );
		}

		System.out.println( "Located and parsed "+units.size()+" persistence units; checking each" );

		if ( persistenceUnitName == null && units.size() > 1 ) {
			// no persistence-unit name to look for was given and we found multiple persistence-units
			throw new PersistenceException( "No name provided and multiple persistence units found" );
		}

		for ( ParsedPersistenceXmlDescriptor persistenceUnit : units ) {
			System.out.println(
					String.format(
					"Checking persistence-unit [name=%s, explicit-provider=%s] against incoming persistence unit name [%s]",
					persistenceUnit.getName(),
					persistenceUnit.getProviderClassName(),
					persistenceUnitName
					)
			);

			final boolean matches = persistenceUnitName == null || persistenceUnit.getName().equals( persistenceUnitName );
			if ( !matches ) {
				System.out.println( "Excluding from consideration due to name mis-match" );
				continue;
			}

			// See if we (Hibernate) are the persistence provider
			if ( ! ProviderChecker.isProvider( persistenceUnit, properties ) ) {
				System.out.println( "Excluding from consideration due to provider mis-match" );
				continue;
			}

			if (providedClassLoaderService != null) {
				return getEntityManagerFactoryBuilder( persistenceUnit, integration, providedClassLoaderService );
			}
			else {
				return getEntityManagerFactoryBuilder( persistenceUnit, integration, providedClassLoader );
			}
		}

		System.out.println( "Found no matching persistence units" );
		return null;
	}
}
