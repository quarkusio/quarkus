/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hibernate.protean.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;
import org.hibernate.protean.impl.serviceregistry.PreconfiguredServiceRegistryBuilder;
import org.hibernate.protean.recording.RecordedState;

import org.jboss.logging.Logger;

/**
 * This can not inherit from HibernatePersistenceProvider as that would force the native-image tool
 * to include all code which could be triggered from using that:
 * we need to be able to fully exclude HibernatePersistenceProvider from the native image.
 */
final class FastbootHibernateProvider implements PersistenceProvider  {

	private static final Logger log = Logger.getLogger( FastbootHibernateProvider.class );

	private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	@Override
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		log.tracef( "Starting createEntityManagerFactory for persistenceUnitName %s", persistenceUnitName );
		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties );
		if ( builder == null ) {
			log.trace( "Could not obtain matching EntityManagerFactoryBuilder, returning null" );
			return null;
		}
		else {
			return builder.build();
		}
	}

	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		log.tracef( "Starting createContainerEntityManagerFactory : %s", info.getPersistenceUnitName() );

		return getEntityManagerFactoryBuilder( info, properties ).build();
	}

	@Override
	public void generateSchema(PersistenceUnitInfo info, Map map) {
		log.tracef( "Starting generateSchema : PUI.name=%s", info.getPersistenceUnitName() );

		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilder( info, map );
		builder.generateSchema();
	}

	@Override
	public boolean generateSchema(String persistenceUnitName, Map map) {
		log.tracef( "Starting generateSchema for persistenceUnitName %s", persistenceUnitName );

		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilderOrNull( persistenceUnitName, map );
		if ( builder == null ) {
			log.trace( "Could not obtain matching EntityManagerFactoryBuilder, returning false" );
			return false;
		}
		builder.generateSchema();
		return true;
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return providerUtil;
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(PersistenceUnitInfo info, Map integration) {
		throw new UnsupportedOperationException( "Not implemented" );
	}

	/**
	 * Copied and modified from HibernatePersistenceProvider#getEntityManagerFactoryBuilderOrNull(String, Map, ClassLoader, ClassLoaderService)
	 * Notable changes:
	 *  - ignore the ClassLoaderService parameter to inject our own custom implementation instead
	 *  - verify the Map properties are not set (or fail as we can't support runtime overrides)
	 *  - don't try looking for ParsedPersistenceXmlDescriptor resources to parse, just take the pre-parsed ones from the static final field
	 *  - class annotations metadata is also injected
	 */
	private EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map properties) {
		log.tracef( "Attempting to obtain correct EntityManagerFactoryBuilder for persistenceUnitName : %s", persistenceUnitName );

		verifyProperties( properties );
		Map integration = Collections.emptyMap();

		//These are pre-parsed during image generation:
		final List<PersistenceUnitDescriptor> units = PersistenceUnitsHolder.getPersistenceUnitDescriptors();

		log.debugf( "Located %s persistence units; checking each", units.size() );

		if ( persistenceUnitName == null && units.size() > 1 ) {
			// no persistence-unit name to look for was given and we found multiple persistence-units
			throw new PersistenceException( "No name provided and multiple persistence units found" );
		}

		for ( PersistenceUnitDescriptor persistenceUnit : units ) {
			log.debugf(
					"Checking persistence-unit [name=%s, explicit-provider=%s] against incoming persistence unit name [%s]",
					persistenceUnit.getName(),
					persistenceUnit.getProviderClassName(),
					persistenceUnitName
			);

			final boolean matches = persistenceUnitName == null || persistenceUnit.getName().equals( persistenceUnitName );
			if ( !matches ) {
				log.debugf( "Excluding from consideration '%s' due to name mis-match", persistenceUnit.getName() );
				continue;
			}

			// See if we (Hibernate) are the persistence provider
			if ( ! isProvider( persistenceUnit ) ) {
				log.debug( "Excluding from consideration due to provider mis-match" );
				continue;
			}

			RecordedState rs = PersistenceUnitsHolder.getMetadata( persistenceUnitName );

			final MetadataImplementor metadata = rs.getFullMeta();

			final Map<String,Object> configurationValues = rs.getConfigurationProperties();
			//TODO:
			final Object validatorFactory = null;
			//TODO:
			final Object cdiBeanManager = null;

			StandardServiceRegistry standardServiceRegistry = rewireMetadataAndExtractServiceRegistry( configurationValues, rs );

			return new FastBootEntityManagerFactoryBuilder( metadata /*Uses the StandardServiceRegistry references by this!*/,
															persistenceUnitName, standardServiceRegistry /*Mostly ignored! (yet needs to match)*/,
															configurationValues,
															validatorFactory,
															cdiBeanManager
			);
		}

		log.debug( "Found no matching persistence units" );
		return null;
	}

	private StandardServiceRegistry rewireMetadataAndExtractServiceRegistry(
			Map<String, Object> configurationValues,
			RecordedState rs) {
		PreconfiguredServiceRegistryBuilder serviceRegistryBuilder = new PreconfiguredServiceRegistryBuilder( rs );
		configurationValues.forEach( (key, value) -> {
			serviceRegistryBuilder.applySetting( key, value );
		} );
		//TODO serviceRegistryBuilder.addInitiator(  )
		//TODO serviceRegistryBuilder.applyIntegrator(  )
		//TODO serviceregistryBuilder.addService(  )


		StandardServiceRegistryImpl standardServiceRegistry = serviceRegistryBuilder.buildNewServiceRegistry();
		return standardServiceRegistry;
	}

	private boolean isProvider(PersistenceUnitDescriptor persistenceUnit) {
		Map<Object, Object> props = Collections.emptyMap();
		String requestedProviderName = extractRequestedProviderName( persistenceUnit, props );
		if ( requestedProviderName == null ) {
			//We'll always assume we are the best possible provider match unless the user explicitly asks for a different one.
			return true;
		}
		return FastbootHibernateProvider.class.getName().equals( requestedProviderName ) || "org.hibernate.jpa.HibernatePersistenceProvider".equals( requestedProviderName );
	}

	public static String extractRequestedProviderName(PersistenceUnitDescriptor persistenceUnit, Map integration) {
		final String integrationProviderName = extractProviderName( integration );
		if ( integrationProviderName != null ) {
			log.debugf( "Integration provided explicit PersistenceProvider [%s]", integrationProviderName );
			return integrationProviderName;
		}

		final String persistenceUnitRequestedProvider = extractProviderName( persistenceUnit );
		if ( persistenceUnitRequestedProvider != null ) {
			log.debugf(
					"Persistence-unit [%s] requested PersistenceProvider [%s]",
					persistenceUnit.getName(),
					persistenceUnitRequestedProvider
			);
			return persistenceUnitRequestedProvider;
		}

		// NOTE : if no provider requested we assume we are the provider (the calls got to us somehow...)
		log.debug( "No PersistenceProvider explicitly requested, assuming Hibernate" );
		return FastbootHibernateProvider.class.getName();
	}


	private static String extractProviderName(Map integration) {
		if ( integration == null ) {
			return null;
		}
		final String setting = (String) integration.get( AvailableSettings.JPA_PERSISTENCE_PROVIDER );
		return setting == null ? null : setting.trim();
	}

	private static String extractProviderName(PersistenceUnitDescriptor persistenceUnit) {
		final String persistenceUnitRequestedProvider = persistenceUnit.getProviderClassName();
		return persistenceUnitRequestedProvider == null ? null : persistenceUnitRequestedProvider.trim();
	}

	private void verifyProperties(Map properties) {
		if ( properties != null && properties.size() != 0 ) {
			throw new PersistenceException( "The FastbootHibernateProvider PersistenceProvider can not support runtime provided properties. " +
													"Make sure you set all properties you need in the configuration resources before building the application." );
		}
	}

	private final ProviderUtil providerUtil = new ProviderUtil() {
		@Override
		public LoadState isLoadedWithoutReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithoutReference( proxy, property, cache );
		}
		@Override
		public LoadState isLoadedWithReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithReference( proxy, property, cache );
		}
		@Override
		public LoadState isLoaded(Object o) {
			return PersistenceUtilHelper.isLoaded(o);
		}
	};

}
