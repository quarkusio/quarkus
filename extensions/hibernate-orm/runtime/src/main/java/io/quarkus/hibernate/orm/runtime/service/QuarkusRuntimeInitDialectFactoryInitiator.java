package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;
import java.util.Optional;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;

public class QuarkusRuntimeInitDialectFactoryInitiator implements StandardServiceInitiator<DialectFactory> {

    private final String persistenceUnitName;
    private final Dialect dialect;
    private final Optional<String> datasourceName;
    private final Optional<DatabaseVersion> buildTimeDbVersion;

    public QuarkusRuntimeInitDialectFactoryInitiator(String persistenceUnitName, Dialect dialect,
            RecordedConfig recordedConfig) {
        this.persistenceUnitName = persistenceUnitName;
        this.dialect = dialect;
        this.datasourceName = recordedConfig.getDataSource();
        this.buildTimeDbVersion = recordedConfig.getDbVersion().isPresent()
                // This is the same version, but parsed in a dialect-specific way.
                ? Optional.of(dialect.getVersion())
                : Optional.empty();
    }

    @Override
    public Class<DialectFactory> getServiceInitiated() {
        return DialectFactory.class;
    }

    @Override
    public DialectFactory initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
        return new QuarkusRuntimeInitDialectFactory(persistenceUnitName, dialect, datasourceName, buildTimeDbVersion);
    }
}
