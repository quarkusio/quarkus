package org.hibernate.protean.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.PersistenceException;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.protean.nativeprocessing.NoConnectionProvider;

final class PersistenceUnitsHolder {

	static final List<ParsedPersistenceXmlDescriptor> units = loadPersistenceUnits();

	private static final Map<String,MetadataImplementor> metadata = constructMetadataAdvance();

	private static final Object NO_NAME_TOKEN = new Object();

	private static List<ParsedPersistenceXmlDescriptor> loadPersistenceUnits() {
		try {
			//Enforce the persistence.xml configuration to be interpreted literally without allowing runtime overrides;
			//(check for the runtime provided properties to be empty as well)
			Map<Object, Object> configurationOverrides = Collections.emptyMap();
			return PersistenceXmlParser.locatePersistenceUnits( configurationOverrides );
		}
		catch (Exception e) {
			throw new PersistenceException( "Unable to locate persistence units", e );
		}
	}

	private static Map<String,MetadataImplementor> constructMetadataAdvance() {
		Map all = new HashMap(  );
		for ( ParsedPersistenceXmlDescriptor unit : units ) {
			MetadataImplementor m = createMetadata( unit );
			Object previous = all.put( unitName( unit ), m );
			if ( previous != null ) {
				throw new IllegalStateException( "Duplicate persistence unit name: " + unit.getName() );
			}
		}
		return all;
	}

	public static MetadataImplementor getMetadata(String persistenceUnitName) {
		Object key = persistenceUnitName;
		if ( persistenceUnitName == null ) {
			key = NO_NAME_TOKEN;
		}
		return metadata.get( key );
	}

	private static Object unitName(ParsedPersistenceXmlDescriptor unit) {
		String name = unit.getName();
		if ( name == null ) {
			return NO_NAME_TOKEN;
		}
		return name;
	}

	private static MetadataImplementor createMetadata(ParsedPersistenceXmlDescriptor unit) {
		final Map nativeImageProcessingProperties = createNativeImageProcessingConfiguration();
		EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
				unit,
				nativeImageProcessingProperties,
				FlatClassLoaderService.INSTANCE
		);
		return entityManagerFactoryBuilder.triggerMetadataBuild();
	}

	private static Map createNativeImageProcessingConfiguration() {
		HashMap props = new HashMap();
		NoConnectionProvider.registerToProperties( props );
		return props;
	}

}
