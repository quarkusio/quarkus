package org.hibernate.protean.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.PersistenceException;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;

final class PersistenceUnitsHolder {

	static final List<ParsedPersistenceXmlDescriptor> units = loadPersistenceUnits();

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
}
