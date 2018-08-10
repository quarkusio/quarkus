package org.hibernate.protean.impl.serviceregistry;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.spi.ServiceRegistryImplementor;

final class ProteanJdbcEnvironmentInitiator implements StandardServiceInitiator<JdbcEnvironment> {

	private final Dialect dialect;

	public ProteanJdbcEnvironmentInitiator(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public Class<JdbcEnvironment> getServiceInitiated() {
		return JdbcEnvironment.class;
	}

	@Override
	public JdbcEnvironment initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new JdbcEnvironmentImpl( registry, dialect );
	}

}