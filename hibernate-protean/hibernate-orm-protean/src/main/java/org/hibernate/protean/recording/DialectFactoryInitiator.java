package org.hibernate.protean.recording;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Copied from org.hibernate.engine.jdbc.dialect.internal.DialectFactoryInitiator
 */
public class DialectFactoryInitiator implements StandardServiceInitiator<DialectFactory> {

	public static final DialectFactoryInitiator INSTANCE = new DialectFactoryInitiator();

	@Override
	public Class<DialectFactory> getServiceInitiated() {
		return DialectFactory.class;
	}

	@Override
	public DialectFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new RecordingDialectFactory();
	}
}