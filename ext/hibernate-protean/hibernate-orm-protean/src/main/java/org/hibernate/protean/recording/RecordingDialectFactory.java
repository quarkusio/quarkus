package org.hibernate.protean.recording;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.service.spi.ServiceRegistryAwareService;

public final class RecordingDialectFactory extends DialectFactoryImpl implements DialectFactory, ServiceRegistryAwareService {

	private Dialect dialect;

	@Override
	public Dialect buildDialect(Map configValues, DialectResolutionInfoSource resolutionInfoSource) throws HibernateException {
		dialect = super.buildDialect( configValues, resolutionInfoSource );
		return dialect;
	}

	public Dialect getDialect() {
		return dialect;
	}

}
