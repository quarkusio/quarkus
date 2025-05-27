package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;
import java.util.Optional;

import jakarta.persistence.PersistenceException;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfigPersistenceUnit;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;

public class QuarkusRuntimeInitDialectFactoryInitiator implements StandardServiceInitiator<DialectFactory> {

    private final String persistenceUnitName;
    private final boolean isFromPersistenceXml;
    private final Dialect dialect;
    private final Optional<String> datasourceName;
    private final DatabaseVersion buildTimeDbVersion;
    private final boolean versionCheckEnabled;

    public QuarkusRuntimeInitDialectFactoryInitiator(String persistenceUnitName,
            boolean isFromPersistenceXml, Dialect dialect,
            RecordedConfig recordedConfig,
            HibernateOrmRuntimeConfigPersistenceUnit runtimePuConfig) {
        this.persistenceUnitName = persistenceUnitName;
        this.isFromPersistenceXml = isFromPersistenceXml;
        this.dialect = dialect;
        this.datasourceName = recordedConfig.getDataSource();
        // We set the version from the dialect since if it wasn't provided explicitly through the `recordedConfig.getDbVersion()`
        // then the version from `DialectVersions.Defaults` will be used:
        this.buildTimeDbVersion = dialect.getVersion();
        HibernateOrmRuntimeConfigPersistenceUnit.HibernateOrmConfigPersistenceUnitDatabase database = runtimePuConfig
                .database();

        if (database.startOffline() && database.versionCheckEnabled().filter(v -> v.booleanValue()).isPresent()) {
            throw new PersistenceException(
                    "When using offline mode `quarkus.hibernate-orm.database.start-offline=true`, version check `quarkus.hibernate-orm.database.version-check.enabled` must be unset or set to `false`");
        }

        this.versionCheckEnabled = runtimePuConfig.database().versionCheckEnabled()
                .orElse(!database.startOffline());
    }

    @Override
    public Class<DialectFactory> getServiceInitiated() {
        return DialectFactory.class;
    }

    @Override
    public DialectFactory initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
        return new QuarkusRuntimeInitDialectFactory(persistenceUnitName, isFromPersistenceXml, dialect, datasourceName,
                buildTimeDbVersion, versionCheckEnabled);
    }
}
