package org.hibernate.protean.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.PersistenceException;

import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.protean.recording.RecordedState;

public final class PersistenceUnitsHolder {

	private static volatile PUStatus COMPACT_UNITS = null;

	private static final Object NO_NAME_TOKEN = new Object();

	/**
	 * Initialized JPA for use in a native image. This must be called from within a static init method.
	 *
	 * In general the <code>parsedPersistenceXmlDescriptors</code> will be provided by caling {@link #loadOriginalXMLParsedDescriptors()}.
	 *
	 * The scanner may be null to use to default scanner, or a custom scanner can be used to stop hibernate scanning
	 * @param parsedPersistenceXmlDescriptors
	 * @param scanner
	 */
	public static void initializeJpa(List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors, Scanner scanner) {
		final List<PersistenceUnitDescriptor> units = convertPersistenceUnits( parsedPersistenceXmlDescriptors );
		final Map<String,RecordedState> metadata = constructMetadataAdvance( parsedPersistenceXmlDescriptors , scanner);
		COMPACT_UNITS = new PUStatus( units, metadata );
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

	public static List<ParsedPersistenceXmlDescriptor> loadOriginalXMLParsedDescriptors() {
		//Enforce the persistence.xml configuration to be interpreted literally without allowing runtime overrides;
		//(check for the runtime provided properties to be empty as well)
		Map<Object, Object> configurationOverrides = Collections.emptyMap();
		List<ParsedPersistenceXmlDescriptor> ret = PersistenceXmlParser.locatePersistenceUnits(configurationOverrides);
		return ret;
	}

	private static Map<String,RecordedState> constructMetadataAdvance(final List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors, Scanner scanner) {
		Map all = new HashMap(  );
		for ( PersistenceUnitDescriptor unit : parsedPersistenceXmlDescriptors ) {
			RecordedState m = createMetadata( unit , scanner);
			Object previous = all.put( unitName( unit ), m );
			if ( previous != null ) {
				throw new IllegalStateException( "Duplicate persistence unit name: " + unit.getName() );
			}
		}
		return all;
	}

	static RecordedState getMetadata(String persistenceUnitName) {
		if(COMPACT_UNITS == null) {
			throw new RuntimeException("JPA not initialized yet");
		}
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

	private static RecordedState createMetadata(PersistenceUnitDescriptor unit, Scanner scanner) {
		FastBootMetadataBuilder fastBootMetadataBuilder = new FastBootMetadataBuilder( unit, scanner );
		return fastBootMetadataBuilder.build();
	}

	// Not a public contract but used by Shamrock
	public static List<PersistenceUnitDescriptor> getPersistenceUnitDescriptors() {
		if(COMPACT_UNITS == null) {
			throw new RuntimeException("JPA not initialized yet");
		}
		return COMPACT_UNITS.units;
	}

	private static class PUStatus {

		private final List<PersistenceUnitDescriptor> units;
		private final Map<String, RecordedState> metadata;

		public PUStatus(final List<PersistenceUnitDescriptor> units, final Map<String, RecordedState> metadata) {
			this.units = units;
			this.metadata = metadata;
		}

	}

}
