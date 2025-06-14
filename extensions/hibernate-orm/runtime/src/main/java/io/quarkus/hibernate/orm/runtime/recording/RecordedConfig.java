package io.quarkus.hibernate.orm.runtime.recording;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.hibernate.orm.runtime.config.DatabaseOrmCompatibilityVersion;
import io.quarkus.hibernate.orm.runtime.customized.BuiltinFormatMapperBehaviour;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * A minimal subset of PU config recorded at build time for use during static init.
 */
public class RecordedConfig {
    private final Optional<String> dataSource;
    private final Optional<String> dbKind;
    private final Optional<String> dbVersion;
    private final Optional<String> explicitDialect;
    private final MultiTenancyStrategy multiTenancyStrategy;
    private final Map<String, String> quarkusConfigUnsupportedProperties;
    private final DatabaseOrmCompatibilityVersion databaseOrmCompatibilityVersion;
    private final BuiltinFormatMapperBehaviour builtinFormatMapperBehaviour;

    @RecordableConstructor
    public RecordedConfig(Optional<String> dataSource, Optional<String> dbKind,
            Optional<String> dbVersion, Optional<String> explicitDialect,
            MultiTenancyStrategy multiTenancyStrategy,
            DatabaseOrmCompatibilityVersion databaseOrmCompatibilityVersion,
            BuiltinFormatMapperBehaviour builtinFormatMapperBehaviour,
            Map<String, String> quarkusConfigUnsupportedProperties) {
        Objects.requireNonNull(dataSource);
        Objects.requireNonNull(dbKind);
        Objects.requireNonNull(dbVersion);
        Objects.requireNonNull(multiTenancyStrategy);
        this.dataSource = dataSource;
        this.dbKind = dbKind;
        this.dbVersion = dbVersion;
        this.explicitDialect = explicitDialect;
        this.multiTenancyStrategy = multiTenancyStrategy;
        this.databaseOrmCompatibilityVersion = databaseOrmCompatibilityVersion;
        this.builtinFormatMapperBehaviour = builtinFormatMapperBehaviour;
        this.quarkusConfigUnsupportedProperties = quarkusConfigUnsupportedProperties;
    }

    public Optional<String> getDataSource() {
        return dataSource;
    }

    public Optional<String> getDbKind() {
        return dbKind;
    }

    public Optional<String> getDbVersion() {
        return dbVersion;
    }

    public Optional<String> getExplicitDialect() {
        return explicitDialect;
    }

    public MultiTenancyStrategy getMultiTenancyStrategy() {
        return multiTenancyStrategy;
    }

    public DatabaseOrmCompatibilityVersion getDatabaseOrmCompatibilityVersion() {
        return databaseOrmCompatibilityVersion;
    }

    public BuiltinFormatMapperBehaviour getBuiltinFormatMapperBehaviour() {
        return builtinFormatMapperBehaviour;
    }

    public Map<String, String> getQuarkusConfigUnsupportedProperties() {
        return quarkusConfigUnsupportedProperties;
    }
}
