package org.hibernate.protean.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.PersistenceException;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

final class PersistenceUnitsHolder {

	private static final PUStatus COMPACT_UNITS = builderPuStatus();

	private static final Object NO_NAME_TOKEN = new Object();

	private static PUStatus builderPuStatus() {
		final List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors = loadOriginalXMLParsedDescriptors();
		final List<PersistenceUnitDescriptor> units = convertPersistenceUnits( parsedPersistenceXmlDescriptors );
		final Map<String,MetadataImplementor> metadata = constructMetadataAdvance( parsedPersistenceXmlDescriptors );
		return new PUStatus(units, metadata);
	}

	private static List<PersistenceUnitDescriptor> convertPersistenceUnits(final List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors) {
		try {
			return parsedPersistenceXmlDescriptors
					.stream()
					.map( LightPersistenceXmlDescriptor::new )
					.collect( Collectors.toList() );
		}
		catch (Exception e) {
			throw new PersistenceException( "Unable to locate persistence units", e );
		}
	}

	private static List<ParsedPersistenceXmlDescriptor> loadOriginalXMLParsedDescriptors() {
		//Enforce the persistence.xml configuration to be interpreted literally without allowing runtime overrides;
		//(check for the runtime provided properties to be empty as well)
		Map<Object, Object> configurationOverrides = Collections.emptyMap();
		return PersistenceXmlParser.locatePersistenceUnits( configurationOverrides );
	}

	private static Map<String,MetadataImplementor> constructMetadataAdvance(final List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors) {
		Map all = new HashMap(  );
		for ( PersistenceUnitDescriptor unit : parsedPersistenceXmlDescriptors ) {
			MetadataImplementor m = createMetadata( unit );
			Object previous = all.put( unitName( unit ), m );
			if ( previous != null ) {
				throw new IllegalStateException( "Duplicate persistence unit name: " + unit.getName() );
			}
		}
		return all;
	}

	static MetadataImplementor getMetadata(String persistenceUnitName) {
		Object key = persistenceUnitName;
		if ( persistenceUnitName == null ) {
			key = NO_NAME_TOKEN;
		}
		return COMPACT_UNITS.metadata.get( key );
	}

	private static Object unitName(PersistenceUnitDescriptor unit) {
		String name = unit.getName();
		if ( name == null ) {
			return NO_NAME_TOKEN;
		}
		return name;
	}

	private static MetadataImplementor createMetadata(PersistenceUnitDescriptor unit) {
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
		props.put( "hibernate.temp.use_jdbc_metadata_defaults", "false" );
		return props;
	}

	static List<PersistenceUnitDescriptor> getPersistenceUnitDescriptors() {
		return COMPACT_UNITS.units;
	}

	private static class PUStatus {

		private final List<PersistenceUnitDescriptor> units;
		private final Map<String, MetadataImplementor> metadata;

		public PUStatus(final List<PersistenceUnitDescriptor> units, final Map<String, MetadataImplementor> metadata) {
			this.units = units;
			this.metadata = metadata;
		}

	}

}
