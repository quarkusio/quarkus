package io.quarkus.hibernate.orm.deployment.util;

import static org.hibernate.cfg.DialectSpecificSettings.MYSQL_BYTES_PER_CHARACTER;
import static org.hibernate.cfg.DialectSpecificSettings.MYSQL_NO_BACKSLASH_ESCAPES;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_APPLICATION_CONTINUITY;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_AUTONOMOUS_DATABASE;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_EXTENDED_STRING_SIZE;
import static org.hibernate.cfg.DialectSpecificSettings.SQL_SERVER_COMPATIBILITY_LEVEL;
import static org.hibernate.cfg.SchemaToolingSettings.STORAGE_ENGINE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import jakarta.persistence.SharedCacheMode;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.common.runtime.DatabaseKind.SupportedDatabaseKind;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.SqlLoadScriptDefaultBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.cache.QuarkusPersistenceUnitCacheConfiguration;
import io.quarkus.hibernate.orm.runtime.schema.InitScriptSupport;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Shared logic for Hibernate ORM and Hibernate Reactive deployment processors:
 * configuring properties, dialects, and SQL load scripts.
 *
 * @see io.quarkus.hibernate.orm.deployment.component.PersistenceUnitDefinitionSupport
 * @see HibernateProcessorUtil
 */
public final class HibernateProcessorSupport {
    private static final Logger LOG = Logger.getLogger(HibernateProcessorSupport.class);

    private HibernateProcessorSupport() {
    }

    public static final String NO_SQL_LOAD_SCRIPT_FILE = "no-file";

    private static final String DEFAULT_SCHEMA_INIT_SCRIPT = "import.sql";
    private static final String DEFAULT_DATA_INIT_SCRIPT = "data.sql";
    private static final String SQL_LOAD_SCRIPT_PROPERTY = "sql-load-script";
    private static final String SCHEMA_INIT_SCRIPT_PROPERTY = "schema-management.init-script";
    private static final String DATA_INIT_SCRIPT_PROPERTY = "data-management.init-script";

    public static Optional<SupportedDatabaseKind> setDialectAndStorageEngine(
            String persistenceUnitName,
            Optional<String> dbKind,
            Optional<String> explicitDialect,
            Optional<String> dbVersion,
            HibernateOrmConfigPersistenceUnit.HibernateOrmConfigPersistenceUnitDialect dialectConfig,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems,
            BiConsumer<String, String> puPropertiesCollector) {
        Optional<String> dialect = explicitDialect;
        Optional<String> dbProductName = Optional.empty();
        Optional<String> dbProductVersion = dbVersion;

        if (dbKind.isPresent() || explicitDialect.isPresent()) {
            for (DatabaseKindDialectBuildItem item : dbKindDialectBuildItems) {
                if (dbKind.isPresent() && DatabaseKind.is(dbKind.get(), item.getDbKind())
                        || explicitDialect.isPresent() && item.getMatchingDialects().contains(explicitDialect.get())) {
                    if (dbKind.isEmpty()) {
                        dbKind = Optional.ofNullable(item.getDbKind());
                    }
                    dbProductName = item.getDatabaseProductName();
                    if (dbProductName.isEmpty() && explicitDialect.isEmpty()) {
                        dialect = item.getDialectOptional();
                    }
                    if (dbVersion.isEmpty()) {
                        dbProductVersion = item.getDefaultDatabaseProductVersion();
                    }
                    break;
                }
            }
            if (dialect.isEmpty() && dbProductName.isEmpty()) {
                throw new ConfigurationException(
                        "Could not guess the dialect from the database kind '"
                                + dbKind.get()
                                + "'. Add an explicit '"
                                + HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "dialect")
                                + "' property.");
            }
        }

        if (dialect.isPresent()) {
            puPropertiesCollector.accept(AvailableSettings.DIALECT, dialect.get());
        } else if (dbProductName.isPresent()) {
            puPropertiesCollector.accept(AvailableSettings.JAKARTA_HBM2DDL_DB_NAME, dbProductName.get());
        }

        if (dbProductVersion.isPresent()) {
            puPropertiesCollector.accept(AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION, dbProductVersion.get());
        }

        Optional<SupportedDatabaseKind> supportedDbKind = dbKind.flatMap(SupportedDatabaseKind::from);

        // dialectConfig is null for persistence units contributed through the SPI, which do not support
        // storage-engine or dialect-specific customization.
        if (dialectConfig != null) {
            handleDialectSpecificSettings(
                    persistenceUnitName,
                    puPropertiesCollector,
                    dialectConfig,
                    supportedDbKind);
        }

        return supportedDbKind;
    }

    private static void handleDialectSpecificSettings(
            String persistenceUnitName,
            BiConsumer<String, String> puPropertiesCollector,
            HibernateOrmConfigPersistenceUnit.HibernateOrmConfigPersistenceUnitDialect dialectConfig,
            Optional<SupportedDatabaseKind> databaseKind) {
        handleStorageEngine(databaseKind, persistenceUnitName, dialectConfig, puPropertiesCollector);

        if (dialectConfig.mariadb().bytesPerCharacter().isPresent()
                || dialectConfig.mariadb().noBackslashEscapes().isPresent()) {
            if (databaseKind.isPresent() && databaseKind.get() != SupportedDatabaseKind.MARIADB) {
                LOG.warnf("MariaDB specific settings being ignored because the database is not MariaDB.");
            } else {
                applyOptionalIntegerSetting(dialectConfig.mariadb().bytesPerCharacter(), MYSQL_BYTES_PER_CHARACTER,
                        puPropertiesCollector);
                applyOptionalBooleanSetting(dialectConfig.mariadb().noBackslashEscapes(), MYSQL_NO_BACKSLASH_ESCAPES,
                        puPropertiesCollector);
            }
        }

        if (dialectConfig.mysql().bytesPerCharacter().isPresent()
                || dialectConfig.mysql().noBackslashEscapes().isPresent()) {
            if (databaseKind.isPresent() && databaseKind.get() != SupportedDatabaseKind.MYSQL) {
                LOG.warnf("MySQL specific settings being ignored because the database is not MySQL.");
            } else {
                applyOptionalIntegerSetting(dialectConfig.mysql().bytesPerCharacter(), MYSQL_BYTES_PER_CHARACTER,
                        puPropertiesCollector);
                applyOptionalBooleanSetting(dialectConfig.mysql().noBackslashEscapes(), MYSQL_NO_BACKSLASH_ESCAPES,
                        puPropertiesCollector);
            }
        }
        if (dialectConfig.oracle().isAnyPropertySet()) {
            if (databaseKind.isPresent() && databaseKind.get() != SupportedDatabaseKind.ORACLE) {
                LOG.warnf("Oracle specific settings being ignored because the database is not Oracle.");
            } else {
                applyOptionalBooleanSetting(dialectConfig.oracle().applicationContinuity(), ORACLE_APPLICATION_CONTINUITY,
                        puPropertiesCollector);
                applyOptionalBooleanSetting(dialectConfig.oracle().autonomous(), ORACLE_AUTONOMOUS_DATABASE,
                        puPropertiesCollector);
                applyOptionalBooleanSetting(dialectConfig.oracle().extended(), ORACLE_EXTENDED_STRING_SIZE,
                        puPropertiesCollector);
            }
        }

        if (dialectConfig.mssql().isAnyPropertySet()) {
            if (databaseKind.isPresent() && databaseKind.get() != SupportedDatabaseKind.MSSQL) {
                LOG.warnf("SQL Server specific settings being ignored because the database is not SQL Server.");
            } else {
                applyOptionalStringSetting(dialectConfig.mssql().compatibilityLevel(), SQL_SERVER_COMPATIBILITY_LEVEL,
                        puPropertiesCollector);
            }
        }
    }

    private static void handleStorageEngine(
            Optional<SupportedDatabaseKind> supportedDatabaseKind,
            String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit.HibernateOrmConfigPersistenceUnitDialect dialectConfig,
            BiConsumer<String, String> puPropertiesCollector) {

        final String mariaDbStorageEngine = dialectConfig.mariadb().storageEngine().orElse(null);
        final String mysqlDbStorageEngine = dialectConfig.mysql().storageEngine().orElse(null);
        if (supportedDatabaseKind.isPresent()
                && (supportedDatabaseKind.get() == SupportedDatabaseKind.MARIADB ||
                        supportedDatabaseKind.get() == SupportedDatabaseKind.MYSQL)) {
            if (mariaDbStorageEngine != null) {
                addStorageEngine(mariaDbStorageEngine, puPropertiesCollector);
            } else if (mysqlDbStorageEngine != null) {
                addStorageEngine(mysqlDbStorageEngine, puPropertiesCollector);
            }
        } else {
            final String storageEngine;
            final String storageEngineSource;
            if (mariaDbStorageEngine != null) {
                storageEngine = mariaDbStorageEngine;
                storageEngineSource = HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName,
                        "dialect.storage-engine");
            } else if (mysqlDbStorageEngine != null) {
                storageEngine = mysqlDbStorageEngine;
                storageEngineSource = HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName,
                        "dialect.storage-engine");
            } else {
                storageEngine = null;
                storageEngineSource = null;
            }

            if (storageEngine != null) {
                if (supportedDatabaseKind.isPresent()) {
                    LOG.warnf(
                            "The storage engine set through configuration property '%1$s', is being ignored"
                                    + " because the database is neither MySQL nor MariaDB.",
                            storageEngineSource);
                } else {
                    addStorageEngine(storageEngine, puPropertiesCollector);
                }
            }
        }
    }

    private static void addStorageEngine(String storageEngine, BiConsumer<String, String> puPropertiesCollector) {
        puPropertiesCollector.accept(STORAGE_ENGINE, storageEngine);
    }

    private static void applyOptionalStringSetting(
            Optional<String> setting,
            String settingName,
            BiConsumer<String, String> puPropertiesCollector) {
        if (setting.isEmpty()) {
            return;
        }
        puPropertiesCollector.accept(settingName, setting.get());
    }

    private static void applyOptionalIntegerSetting(
            Optional<Integer> setting,
            String settingName,
            BiConsumer<String, String> puPropertiesCollector) {
        if (setting.isEmpty()) {
            return;
        }
        puPropertiesCollector.accept(settingName, Integer.toString(setting.get()));
    }

    private static void applyOptionalBooleanSetting(
            Optional<Boolean> setting,
            String settingName,
            BiConsumer<String, String> puPropertiesCollector) {
        if (setting.isEmpty()) {
            return;
        }
        puPropertiesCollector.accept(settingName, Boolean.toString(setting.get()));
    }

    public static void configureProperties(QuarkusPersistenceUnitDescriptor desc, HibernateOrmConfigPersistenceUnit config,
            HibernateOrmConfig hibernateOrmConfig, boolean reactive) {
        // Quoting strategy
        configureQuoting(desc, config);

        // Physical Naming Strategy
        config.physicalNamingStrategy().ifPresent(namingStrategy -> desc.getProperties()
                .setProperty(AvailableSettings.PHYSICAL_NAMING_STRATEGY, namingStrategy));

        // Implicit Naming Strategy
        config.implicitNamingStrategy().ifPresent(namingStrategy -> desc.getProperties()
                .setProperty(AvailableSettings.IMPLICIT_NAMING_STRATEGY, namingStrategy));

        // Mapping
        if (config.mapping().timezone().timeZoneDefaultStorage().isPresent()) {
            desc.getProperties().setProperty(AvailableSettings.TIMEZONE_DEFAULT_STORAGE,
                    config.mapping().timezone().timeZoneDefaultStorage().get().name());
        }
        desc.getProperties().setProperty(AvailableSettings.PREFERRED_POOLED_OPTIMIZER,
                config.mapping().id().optimizer().idOptimizerDefault()
                        .orElse(HibernateOrmConfigPersistenceUnit.IdOptimizerType.POOLED_LO).configName);

        // Duration
        config.mapping().duration().durationPreferredJdbcType().ifPresent(duration -> desc.getProperties().setProperty(
                AvailableSettings.PREFERRED_DURATION_JDBC_TYPE,
                duration));

        // Instant
        config.mapping().instantPreferredJdbcType().ifPresent(instant -> desc.getProperties().setProperty(
                AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE,
                instant));

        // Boolean
        config.mapping().booleanPreferredJdbcType().ifPresent(
                bool -> desc.getProperties().setProperty(
                        AvailableSettings.PREFERRED_BOOLEAN_JDBC_TYPE,
                        bool));

        // UUID
        config.mapping().UUIDPreferredJdbcType().ifPresent(
                uuid -> desc.getProperties().setProperty(
                        AvailableSettings.PREFERRED_UUID_JDBC_TYPE,
                        uuid));

        //charset
        desc.getProperties()
                .setProperty(AvailableSettings.HBM2DDL_CHARSET_NAME, config.database().charset().name());

        // Query
        int batchSize = config.fetch().batchSize().orElse(defaultBatchSize(reactive));
        if (batchSize > 0) {
            desc.getProperties().setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, Integer.toString(batchSize));
        }

        // Fetch
        if (config.fetch().maxDepth().isPresent()) {
            setMaxFetchDepth(desc, config.fetch().maxDepth());
        }

        desc.getProperties().setProperty(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, Integer.toString(
                config.query().queryPlanCacheMaxSize()));

        desc.getProperties().setProperty(AvailableSettings.DEFAULT_NULL_ORDERING,
                config.query().defaultNullOrdering().name().toLowerCase(Locale.ROOT));

        desc.getProperties().setProperty(AvailableSettings.IN_CLAUSE_PARAMETER_PADDING,
                String.valueOf(config.query().inClauseParameterPadding()));

        desc.getProperties().setProperty(AvailableSettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
                String.valueOf(config.query().failOnPaginationOverCollectionFetch()));

        // Disable sequence validations: they are reportedly slow, and people already get the same validation from normal schema validation
        desc.getProperties().put(AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY,
                SequenceMismatchStrategy.NONE);

        // JDBC
        config.jdbc().timezone().ifPresent(
                timezone -> desc.getProperties().setProperty(AvailableSettings.JDBC_TIME_ZONE, timezone));

        config.jdbc().statementFetchSize().ifPresent(
                fetchSize -> desc.getProperties().setProperty(AvailableSettings.STATEMENT_FETCH_SIZE,
                        String.valueOf(fetchSize)));

        config.jdbc().statementBatchSize().ifPresent(
                fetchSize -> desc.getProperties().setProperty(AvailableSettings.STATEMENT_BATCH_SIZE,
                        String.valueOf(fetchSize)));

        // Statistics
        if (hibernateOrmConfig.metrics().enabled()
                || (hibernateOrmConfig.statistics().isPresent() && hibernateOrmConfig.statistics().get())) {
            desc.getProperties().setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
            //When statistics are enabled, the default in Hibernate ORM is to also log them after each
            // session; turn that off by default as it's very noisy:
            desc.getProperties().setProperty(AvailableSettings.LOG_SESSION_METRICS,
                    String.valueOf(hibernateOrmConfig.logSessionMetrics().orElse(false)));
        }

        // Caching
        configureCaching(desc, config);

        // Validation
        configureValidation(desc, config);

        // Discriminator Column
        desc.getProperties().setProperty(AvailableSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS,
                String.valueOf(config.discriminator().ignoreExplicitForJoined()));
    }

    // TODO ideally we should align on ORM and use 16 as a default, but that would break applications
    //  because of https://github.com/hibernate/hibernate-reactive/issues/742
    private static int defaultBatchSize(boolean reactive) {
        return reactive ? -1 : 16;
    }

    private static void setMaxFetchDepth(PersistenceUnitDescriptor descriptor, OptionalInt maxFetchDepth) {
        descriptor.getProperties().setProperty(AvailableSettings.MAX_FETCH_DEPTH, String.valueOf(maxFetchDepth.getAsInt()));
    }

    private static void configureCaching(QuarkusPersistenceUnitDescriptor descriptor,
            HibernateOrmConfigPersistenceUnit config) {
        Properties p = descriptor.getProperties();
        if (config.secondLevelCachingEnabled()) {
            p.putIfAbsent(AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.TRUE);
            p.putIfAbsent(AvailableSettings.USE_SECOND_LEVEL_CACHE, Boolean.TRUE);
            p.putIfAbsent(AvailableSettings.USE_QUERY_CACHE, Boolean.TRUE);
            p.putIfAbsent(AvailableSettings.JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE);
            p.putIfAbsent(QuarkusPersistenceUnitCacheConfiguration.CONFIG_KEY, toQuarkusCacheConfiguration(config));
        } else {
            p.put(AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.FALSE);
            p.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, Boolean.FALSE);
            p.put(AvailableSettings.USE_QUERY_CACHE, Boolean.FALSE);
            p.put(AvailableSettings.JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.NONE);
        }
    }

    private static QuarkusPersistenceUnitCacheConfiguration toQuarkusCacheConfiguration(
            HibernateOrmConfigPersistenceUnit config) {
        Map<String, QuarkusPersistenceUnitCacheConfiguration.Cache> caches = new HashMap<>();
        for (var regionEntry : config.cache().entrySet()) {
            String cacheName = regionEntry.getKey();
            var cacheConfig = regionEntry.getValue();
            var memory = cacheConfig.memory();

            // Validate mutual exclusivity
            if (memory.objectCount().isPresent() && memory.maximumWeight().isPresent()) {
                throw new IllegalStateException(
                        "Cache region '" + cacheName + "': 'object-count' and 'maximum-weight' are mutually exclusive. "
                                + "Use 'object-count' for count-based eviction or 'maximum-weight' for weight-based eviction.");
            }
            if (memory.weigherClass().isPresent() && memory.maximumWeight().isEmpty()) {
                throw new IllegalStateException(
                        "Cache region '" + cacheName + "': 'weigher-class' requires 'maximum-weight' to be set.");
            }

            caches.put(cacheName, new QuarkusPersistenceUnitCacheConfiguration.Cache(
                    memory.objectCount().orElse(QuarkusPersistenceUnitCacheConfiguration.Cache.DEFAULT.maxSize()),
                    cacheConfig.expiration().maxIdle()
                            .orElse(QuarkusPersistenceUnitCacheConfiguration.Cache.DEFAULT.maxIdle()),
                    memory.maximumWeight().orElse(-1L),
                    memory.weigherClass().orElse(null)));
        }
        return new QuarkusPersistenceUnitCacheConfiguration(caches);
    }

    private static void configureValidation(QuarkusPersistenceUnitDescriptor descriptor,
            HibernateOrmConfigPersistenceUnit config) {
        descriptor.getProperties().setProperty(
                AvailableSettings.JAKARTA_VALIDATION_MODE,
                config.validation().mode()
                        .stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(",")));
    }

    private static void configureQuoting(QuarkusPersistenceUnitDescriptor desc,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig) {
        if (persistenceUnitConfig.quoteIdentifiers()
                .strategy() == HibernateOrmConfigPersistenceUnit.IdentifierQuotingStrategy.ALL
                || persistenceUnitConfig.quoteIdentifiers()
                        .strategy() == HibernateOrmConfigPersistenceUnit.IdentifierQuotingStrategy.ALL_EXCEPT_COLUMN_DEFINITIONS) {
            desc.getProperties().setProperty(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, "true");
        }
        if (persistenceUnitConfig.quoteIdentifiers()
                .strategy() == HibernateOrmConfigPersistenceUnit.IdentifierQuotingStrategy.ALL_EXCEPT_COLUMN_DEFINITIONS) {
            desc.getProperties().setProperty(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS, "true");
        } else if (persistenceUnitConfig.quoteIdentifiers()
                .strategy() == HibernateOrmConfigPersistenceUnit.IdentifierQuotingStrategy.ONLY_KEYWORDS) {
            desc.getProperties().setProperty(AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "true");
        }
    }

    /**
     * Configures the SQL init scripts of a persistence unit:
     * <ul>
     * <li>the data init scripts ({@code quarkus.hibernate-orm.data-management.init-script},
     * {@code data.sql} by default if it exists in the classpath), executed to load data regardless of how the schema
     * is managed;
     * their execution is decided at runtime based on {@code quarkus.hibernate-orm.data-management.strategy},
     * see {@link InitScriptSupport#configureDataManagement};</li>
     * <li>the schema init scripts ({@code quarkus.hibernate-orm.schema-management.init-script},
     * {@code import.sql} by default if it exists in the classpath), executed right after Hibernate ORM created the schema.</li>
     * </ul>
     * Scripts configured through the deprecated {@code quarkus.hibernate-orm.sql-load-script} property
     * keep their historical behavior: they are only executed when Hibernate ORM creates the schema.
     */
    public static void configureInitScripts(String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            WorkspaceModule applicationModule,
            List<SqlLoadScriptDefaultBuildItem> additionalSqlLoadScriptDefaults,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            QuarkusPersistenceUnitDescriptor descriptor) {
        Optional<List<String>> legacySqlLoadScript = persistenceUnitConfig.sqlLoadScript();
        Optional<List<String>> dataInitScript = persistenceUnitConfig.dataManagement().initScript();
        Optional<List<String>> schemaInitScript = persistenceUnitConfig.schemaManagement().initScript();

        // Data init scripts, executed to load data
        InitScriptConfig dataScripts;
        // Schema init scripts, executed right after Hibernate ORM created the schema
        InitScriptConfig schemaScripts;
        if (legacySqlLoadScript.isPresent()) {
            if (dataInitScript.isPresent() || schemaInitScript.isPresent()) {
                throw new ConfigurationException(String.format(Locale.ROOT,
                        "'%s' is deprecated and cannot be used together with '%s' or '%s'."
                                + " Remove it and only use those properties.",
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, SQL_LOAD_SCRIPT_PROPERTY),
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, DATA_INIT_SCRIPT_PROPERTY),
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, SCHEMA_INIT_SCRIPT_PROPERTY)));
            }
            dataScripts = InitScriptConfig.explicit(SQL_LOAD_SCRIPT_PROPERTY, legacySqlLoadScript.get());
            // Historically, an explicit 'sql-load-script' replaced the default 'import.sql' entirely: keep it that way.
            schemaScripts = InitScriptConfig.defaults(SCHEMA_INIT_SCRIPT_PROPERTY, Collections.emptyList());
            // Lets the runtime know that the deprecated property is in use, so that it keeps the historical behavior
            // (see InitScriptSupport).
            descriptor.getProperties().setProperty(InitScriptSupport.LEGACY_SQL_LOAD_SCRIPT, "true");
            if (dataScripts.scripts.isEmpty()) {
                LOG.warnf("Persistence unit '%s' uses the deprecated configuration property '%s'."
                        + " To ignore the default init scripts, set '%s' and '%s' to '%s' instead.",
                        persistenceUnitName,
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, SQL_LOAD_SCRIPT_PROPERTY),
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, SCHEMA_INIT_SCRIPT_PROPERTY),
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, DATA_INIT_SCRIPT_PROPERTY),
                        NO_SQL_LOAD_SCRIPT_FILE);
            } else {
                LOG.warnf("Persistence unit '%s' uses the deprecated configuration property '%s'."
                        + " Use '%s' to load data (executed regardless of how the schema is managed)"
                        + " or '%s' to complete the schema (executed only when Hibernate ORM creates the schema).",
                        persistenceUnitName,
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, SQL_LOAD_SCRIPT_PROPERTY),
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, DATA_INIT_SCRIPT_PROPERTY),
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, SCHEMA_INIT_SCRIPT_PROPERTY));
            }
        } else {
            if (dataInitScript.isPresent()) {
                dataScripts = InitScriptConfig.explicit(DATA_INIT_SCRIPT_PROPERTY, dataInitScript.get());
            } else {
                dataScripts = InitScriptConfig.defaults(DATA_INIT_SCRIPT_PROPERTY,
                        defaultDataInitScripts(persistenceUnitName, additionalSqlLoadScriptDefaults));
            }
            if (schemaInitScript.isPresent()) {
                schemaScripts = InitScriptConfig.explicit(SCHEMA_INIT_SCRIPT_PROPERTY, schemaInitScript.get());
                for (String script : schemaScripts.scripts) {
                    if (script.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                        throw new ConfigurationException(String.format(Locale.ROOT,
                                "Zip files are not supported in '%s=%s'. Reference the SQL files directly.",
                                HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, SCHEMA_INIT_SCRIPT_PROPERTY),
                                String.join(",", schemaScripts.configuredScripts)));
                    }
                }
            } else {
                schemaScripts = InitScriptConfig.defaults(SCHEMA_INIT_SCRIPT_PROPERTY, List.of(DEFAULT_SCHEMA_INIT_SCRIPT));
            }
        }

        List<String> existingDataScripts = registerInitScripts(persistenceUnitName, dataScripts,
                applicationArchivesBuildItem, applicationModule, nativeImageResources, hotDeploymentWatchedFiles);
        if (!existingDataScripts.isEmpty()) {
            descriptor.getProperties().setProperty(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE,
                    String.join(",", existingDataScripts));
        }

        List<String> existingSchemaScripts = registerInitScripts(persistenceUnitName, schemaScripts,
                applicationArchivesBuildItem, applicationModule, nativeImageResources, hotDeploymentWatchedFiles);
        if (!existingSchemaScripts.isEmpty()) {
            descriptor.getProperties().setProperty(AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE,
                    String.join(",", existingSchemaScripts));
            descriptor.getProperties().setProperty(AvailableSettings.JAKARTA_HBM2DDL_CREATE_SOURCE,
                    InitScriptSupport.CREATE_SOURCE_METADATA_THEN_SCRIPT);
        }

        //Disable implicit loading of the default import script (import.sql)
        descriptor.getProperties().setProperty(AvailableSettings.HBM2DDL_SKIP_DEFAULT_IMPORT_FILE, "true");
    }

    private static List<String> defaultDataInitScripts(String persistenceUnitName,
            List<SqlLoadScriptDefaultBuildItem> additionalDefaults) {
        Set<String> defaults = new LinkedHashSet<>();
        defaults.add(DEFAULT_DATA_INIT_SCRIPT);
        if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            for (SqlLoadScriptDefaultBuildItem additionalDefault : additionalDefaults) {
                defaults.add(additionalDefault.getResourceName());
            }
        }
        return new ArrayList<>(defaults);
    }

    /**
     * Registers the given scripts as native resources and hot-deployment watched files,
     * and returns those that exist in the application archive.
     */
    private static List<String> registerInitScripts(String persistenceUnitName, InitScriptConfig config,
            ApplicationArchivesBuildItem applicationArchivesBuildItem, WorkspaceModule applicationModule,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        List<String> existingScripts = new ArrayList<>();
        for (String script : config.scripts) {
            Path scriptPath;
            try {
                scriptPath = applicationArchivesBuildItem.getRootArchive().getChildPath(script);
            } catch (RuntimeException e) {
                throw new ConfigurationException(
                        "Unable to interpret path referenced in '"
                                + HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, config.propertyName) + "="
                                + String.join(",", config.configuredScripts)
                                + "': " + e.getMessage());
            }

            if (scriptPath != null && !Files.isDirectory(scriptPath)) {
                // enlist resource if present
                existingScripts.add(script);
                nativeImageResources.produce(new NativeImageResourceBuildItem(script));
            } else if (config.explicit) {
                //raise exception if explicit file is not present (i.e. not the default)
                throw new ConfigurationException(
                        unableToFindScriptMessage(persistenceUnitName, config, script, applicationModule));
            }
            // in dev mode we want to make sure that we watch for changes to file even if it doesn't currently exist
            // as a user could still add it after performing the initial configuration
            hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(script));
        }
        return existingScripts;
    }

    private static String unableToFindScriptMessage(String persistenceUnitName, InitScriptConfig config, String script,
            WorkspaceModule applicationModule) {
        String propertyKey = HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, config.propertyName);
        String propertyValue = String.join(",", config.configuredScripts);
        StringBuilder message = new StringBuilder("Unable to find file referenced in '")
                .append(propertyKey).append("=").append(propertyValue)
                .append("'. Remove property or add file to your path.");
        Path testResource = findTestResource(applicationModule, script);
        if (testResource != null) {
            message.append(" The file exists in the test resources ('").append(testResource)
                    .append("'), which are only part of the application in test mode:")
                    .append(" either move it to the main resources,")
                    .append(" or set the property for the test profile only ('%test.").append(propertyKey).append("=")
                    .append(propertyValue).append("').");
        }
        return message.toString();
    }

    /**
     * @return The path to the given script in the test resources of the application module, if any.
     *         Test resources are only part of the application in test mode,
     *         so this is a likely cause of a script not being found in dev mode or in a production build.
     */
    static Path findTestResource(WorkspaceModule applicationModule, String script) {
        if (applicationModule == null) {
            return null;
        }
        ArtifactSources testSources = applicationModule.getTestSources();
        if (testSources == null) {
            return null;
        }
        for (SourceDir resourceDir : testSources.getResourceDirs()) {
            Path dir = resourceDir.getDir();
            if (dir == null) {
                continue;
            }
            Path candidate = dir.resolve(script).normalize();
            if (candidate.startsWith(dir.normalize()) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static final class InitScriptConfig {
        final String propertyName;
        final List<String> configuredScripts;
        final List<String> scripts;
        final boolean explicit;

        private InitScriptConfig(String propertyName, List<String> configuredScripts, List<String> scripts,
                boolean explicit) {
            this.propertyName = propertyName;
            this.configuredScripts = configuredScripts;
            this.scripts = scripts;
            this.explicit = explicit;
        }

        static InitScriptConfig explicit(String propertyName, List<String> configuredScripts) {
            List<String> scripts = new ArrayList<>();
            for (String script : configuredScripts) {
                if (!NO_SQL_LOAD_SCRIPT_FILE.equalsIgnoreCase(script)) {
                    scripts.add(script);
                }
            }
            return new InitScriptConfig(propertyName, configuredScripts, scripts, true);
        }

        static InitScriptConfig defaults(String propertyName, List<String> scripts) {
            return new InitScriptConfig(propertyName, scripts, new ArrayList<>(scripts), false);
        }
    }

}
