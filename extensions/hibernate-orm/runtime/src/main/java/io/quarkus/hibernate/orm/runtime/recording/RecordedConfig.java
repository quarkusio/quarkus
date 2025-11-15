package io.quarkus.hibernate.orm.runtime.recording;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.quarkus.hibernate.orm.runtime.config.DatabaseOrmCompatibilityVersion;
import io.quarkus.hibernate.orm.runtime.customized.BuiltinFormatMapperBehaviour;
import io.quarkus.hibernate.orm.runtime.customized.JsonFormatterCustomizationCheck;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * A minimal subset of PU config recorded at build time for use during static init.
 */
public class RecordedConfig {
    private final Optional<String> dataSource;
    private final Optional<String> dbKind;
    private final Optional<String> supportedDBkind;
    private final Optional<String> dbVersion;
    private final Optional<String> explicitDialect;
    private final Set<String> entityClassNames;
    private final MultiTenancyStrategy multiTenancyStrategy;
    private final Map<String, String> quarkusConfigUnsupportedProperties;
    private final DatabaseOrmCompatibilityVersion databaseOrmCompatibilityVersion;
    private final BuiltinFormatMapperBehaviour builtinFormatMapperBehaviour;
    private final JsonFormatterCustomizationCheck jsonFormatterCustomizationCheck;

    @RecordableConstructor
    public RecordedConfig(Optional<String> dataSource, Optional<String> dbKind,
            Optional<String> supportedDBkind,
            Optional<String> dbVersion, Optional<String> explicitDialect, Set<String> entityClassNames,
            MultiTenancyStrategy multiTenancyStrategy,
            DatabaseOrmCompatibilityVersion databaseOrmCompatibilityVersion,
            BuiltinFormatMapperBehaviour builtinFormatMapperBehaviour,
            JsonFormatterCustomizationCheck jsonFormatterCustomizationCheck,
            Map<String, String> quarkusConfigUnsupportedProperties) {
        Objects.requireNonNull(dataSource);
        Objects.requireNonNull(dbKind);
        Objects.requireNonNull(supportedDBkind);
        Objects.requireNonNull(dbVersion);
        Objects.requireNonNull(multiTenancyStrategy);
        this.dataSource = dataSource;
        this.dbKind = dbKind;
        this.supportedDBkind = supportedDBkind;
        this.dbVersion = dbVersion;
        this.explicitDialect = explicitDialect;
        this.entityClassNames = entityClassNames;
        this.multiTenancyStrategy = multiTenancyStrategy;
        this.databaseOrmCompatibilityVersion = databaseOrmCompatibilityVersion;
        this.builtinFormatMapperBehaviour = builtinFormatMapperBehaviour;
        this.jsonFormatterCustomizationCheck = jsonFormatterCustomizationCheck;
        this.quarkusConfigUnsupportedProperties = quarkusConfigUnsupportedProperties;
    }

    public Optional<String> getDataSource() {
        return dataSource;
    }

    public Optional<String> getDbKind() {
        return dbKind;
    }

    public Optional<String> getSupportedDBkind() {
        return supportedDBkind;
    }

    public Optional<String> getDbVersion() {
        return dbVersion;
    }

    public Optional<String> getExplicitDialect() {
        return explicitDialect;
    }

    public Set<String> getEntityClassNames() {
        return entityClassNames;
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

    public JsonFormatterCustomizationCheck getJsonFormatterCustomizationCheck() {
        return jsonFormatterCustomizationCheck;
    }

    public Map<String, String> getQuarkusConfigUnsupportedProperties() {
        return quarkusConfigUnsupportedProperties;
    }
}
