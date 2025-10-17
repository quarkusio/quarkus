package io.quarkus.hibernate.orm.deployment.util;

import static io.quarkus.hibernate.orm.deployment.HibernateConfigUtil.firstPresent;
import static org.hibernate.cfg.DialectSpecificSettings.MYSQL_BYTES_PER_CHARACTER;
import static org.hibernate.cfg.DialectSpecificSettings.MYSQL_NO_BACKSLASH_ESCAPES;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_APPLICATION_CONTINUITY;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_AUTONOMOUS_DATABASE;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_EXTENDED_STRING_SIZE;
import static org.hibernate.cfg.DialectSpecificSettings.SQL_SERVER_COMPATIBILITY_LEVEL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
import jakarta.persistence.ValidationMode;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.common.runtime.DatabaseKind.SupportedDatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateConfigUtil;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.customized.BuiltinFormatMapperBehaviour;
import io.quarkus.hibernate.orm.runtime.customized.FormatMapperKind;
import io.quarkus.hibernate.orm.runtime.customized.JsonFormatterCustomizationCheck;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * A set of utilities method to collect the common operations needed to configure the
 * Hibernate ORM and Hibernate Reactive extensions.
 */
public final class HibernateProcessorUtil {
    private static final Logger LOG = Logger.getLogger(HibernateProcessorUtil.class);
    public static final String NO_SQL_LOAD_SCRIPT_FILE = "no-file";

    private HibernateProcessorUtil() {
    }

    public static Optional<FormatMapperKind> jsonMapperKind(Capabilities capabilities, BuiltinFormatMapperBehaviour behaviour) {
        if (BuiltinFormatMapperBehaviour.IGNORE.equals(behaviour)) {
            return Optional.empty();
        }
        if (capabilities.isPresent(Capability.JACKSON)) {
            return Optional.of(FormatMapperKind.JACKSON);
        } else if (capabilities.isPresent(Capability.JSONB)) {
            return Optional.of(FormatMapperKind.JSONB);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<FormatMapperKind> xmlMapperKind(Capabilities capabilities, BuiltinFormatMapperBehaviour behaviour) {
        if (BuiltinFormatMapperBehaviour.IGNORE.equals(behaviour)) {
            return Optional.empty();
        }
        return capabilities.isPresent(Capability.JAXB)
                ? Optional.of(FormatMapperKind.JAXB)
                : Optional.empty();
    }

    public static boolean isHibernateValidatorPresent(Capabilities capabilities) {
        return capabilities.isPresent(Capability.HIBERNATE_VALIDATOR);
    }

    public static Optional<SupportedDatabaseKind> setDialectAndStorageEngine(
            String persistenceUnitName,
            Optional<String> dbKind,
            Optional<String> explicitDialect,
            Optional<String> explicitDbMinVersion,
            HibernateOrmConfigPersistenceUnit.HibernateOrmConfigPersistenceUnitDialect dialectConfig,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BiConsumer<String, String> puPropertiesCollector,
            Set<String> storageEngineCollector) {
        Optional<String> dialect = explicitDialect;
        Optional<String> dbProductName = Optional.empty();
        Optional<String> dbProductVersion = explicitDbMinVersion;

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
                    if (explicitDbMinVersion.isEmpty()) {
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

        handleDialectSpecificSettings(
                persistenceUnitName,
                systemProperties,
                puPropertiesCollector,
                storageEngineCollector,
                dialectConfig,
                supportedDbKind);

        return supportedDbKind;
    }

    private static void handleDialectSpecificSettings(
            String persistenceUnitName,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BiConsumer<String, String> puPropertiesCollector,
            Set<String> storageEngineCollector,
            HibernateOrmConfigPersistenceUnit.HibernateOrmConfigPersistenceUnitDialect dialectConfig,
            Optional<SupportedDatabaseKind> databaseKind) {
        handleStorageEngine(databaseKind, persistenceUnitName, dialectConfig, storageEngineCollector,
                systemProperties);

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
            Set<String> storageEngineCollector,
            BuildProducer<SystemPropertyBuildItem> systemProperties) {

        final String topLevelStorageEngine = dialectConfig.storageEngine().orElse(null);

        if (topLevelStorageEngine != null) {
            // NOTE: this top-level storage-engine setting is deprecated - log a warning
            LOG.warnf(
                    "The storage engine set through configuration property '%1$s' is deprecated; "
                            + "use '%2$s' or '%3$s' instead, depending on the database.",
                    HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "dialect.storage-engine"),
                    HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "dialect.mariadb.storage-engine"),
                    HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "dialect.mysql.storage-engine"));
        }

        final String mariaDbStorageEngine = dialectConfig.mariadb().storageEngine().orElse(topLevelStorageEngine);
        final String mysqlDbStorageEngine = dialectConfig.mysql().storageEngine().orElse(topLevelStorageEngine);
        if (supportedDatabaseKind.isPresent()
                && (supportedDatabaseKind.get() == SupportedDatabaseKind.MARIADB ||
                        supportedDatabaseKind.get() == SupportedDatabaseKind.MYSQL)) {
            if (mariaDbStorageEngine != null) {
                addStorageEngine(storageEngineCollector, systemProperties, mariaDbStorageEngine);
            } else if (mysqlDbStorageEngine != null) {
                addStorageEngine(storageEngineCollector, systemProperties, mysqlDbStorageEngine);
            }
        } else {
            final String storageEngine;
            final String storageEngineSource;
            if (topLevelStorageEngine != null) {
                storageEngine = topLevelStorageEngine;
                storageEngineSource = HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "dialect.storage-engine");
            } else if (mariaDbStorageEngine != null) {
                storageEngine = mariaDbStorageEngine;
                storageEngineSource = HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName,
                        "dialect.mariadb.storage-engine");
            } else if (mysqlDbStorageEngine != null) {
                storageEngine = mysqlDbStorageEngine;
                storageEngineSource = HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName,
                        "dialect.mysql.storage-engine");
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
                    systemProperties.produce(new SystemPropertyBuildItem(AvailableSettings.STORAGE_ENGINE, storageEngine));
                    storageEngineCollector.add(storageEngine);
                }
            }
        }
    }

    private static void addStorageEngine(Set<String> storageEngineCollector,
            BuildProducer<SystemPropertyBuildItem> systemProperties, String mariaDbStorageEngine) {
        storageEngineCollector.add(mariaDbStorageEngine);
        systemProperties.produce(new SystemPropertyBuildItem(AvailableSettings.STORAGE_ENGINE, mariaDbStorageEngine));
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

        // Metadata builder contributor
        config.metadataBuilderContributor().ifPresent(className -> desc.getProperties()
                .setProperty(JpaSettings.METADATA_BUILDER_CONTRIBUTOR, className));

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
        int batchSize = firstPresent(config.fetch().batchSize(), config.batchFetchSize()).orElse(defaultBatchSize(reactive));
        if (batchSize > 0) {
            desc.getProperties().setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, Integer.toString(batchSize));
        }

        // Fetch
        if (config.fetch().maxDepth().isPresent()) {
            setMaxFetchDepth(desc, config.fetch().maxDepth());
        } else if (config.maxFetchDepth().isPresent()) {
            setMaxFetchDepth(desc, config.maxFetchDepth());
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

    private static List<String> getSqlLoadScript(Optional<List<String>> sqlLoadScript, LaunchMode launchMode) {
        if (sqlLoadScript.isPresent()) {
            return sqlLoadScript.get().stream()
                    .filter(s -> !NO_SQL_LOAD_SCRIPT_FILE.equalsIgnoreCase(s))
                    .collect(Collectors.toList());
        }
        if (launchMode.isProduction()) {
            return Collections.emptyList();
        }
        return List.of("import.sql");
    }

    private static boolean isMySQLOrMariaDB(Optional<String> dbKind, Optional<String> dialect) {
        if (dbKind.isPresent() && (DatabaseKind.isMySQL(dbKind.get()) || DatabaseKind.isMariaDB(dbKind.get()))) {
            return true;
        }
        if (dialect.isPresent()) {
            String lowercaseDialect = dialect.get().toLowerCase(Locale.ROOT);
            return lowercaseDialect.contains("mysql") || lowercaseDialect.contains("mariadb");
        }
        return false;
    }

    private static void configureCaching(QuarkusPersistenceUnitDescriptor descriptor,
            HibernateOrmConfigPersistenceUnit config) {
        if (config.secondLevelCachingEnabled()) {
            Properties p = descriptor.getProperties();
            p.putIfAbsent(AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.TRUE);
            p.putIfAbsent(AvailableSettings.USE_SECOND_LEVEL_CACHE, Boolean.TRUE);
            p.putIfAbsent(AvailableSettings.USE_QUERY_CACHE, Boolean.TRUE);
            p.putIfAbsent(AvailableSettings.JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE);
            Map<String, String> cacheConfigEntries = HibernateConfigUtil.getCacheConfigEntries(config);
            for (Map.Entry<String, String> entry : cacheConfigEntries.entrySet()) {
                descriptor.getProperties().setProperty(entry.getKey(), entry.getValue());
            }
        } else {
            Properties p = descriptor.getProperties();
            p.put(AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.FALSE);
            p.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, Boolean.FALSE);
            p.put(AvailableSettings.USE_QUERY_CACHE, Boolean.FALSE);
            p.put(AvailableSettings.JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.NONE);
        }
    }

    private static void configureValidation(QuarkusPersistenceUnitDescriptor descriptor,
            HibernateOrmConfigPersistenceUnit config) {
        if (!config.validation().enabled()) {
            descriptor.getProperties().setProperty(AvailableSettings.JAKARTA_VALIDATION_MODE, ValidationMode.NONE.name());
        } else {
            descriptor.getProperties().setProperty(
                    AvailableSettings.JAKARTA_VALIDATION_MODE,
                    config.validation().mode()
                            .stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(",")));
        }
    }

    private static void configureQuoting(QuarkusPersistenceUnitDescriptor desc,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig) {
        if (persistenceUnitConfig.quoteIdentifiers()
                .strategy() == HibernateOrmConfigPersistenceUnit.IdentifierQuotingStrategy.ALL
                || persistenceUnitConfig.quoteIdentifiers()
                        .strategy() == HibernateOrmConfigPersistenceUnit.IdentifierQuotingStrategy.ALL_EXCEPT_COLUMN_DEFINITIONS
                || persistenceUnitConfig.database().globallyQuotedIdentifiers()) {
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

    public static void configureSqlLoadScript(String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig,
            ApplicationArchivesBuildItem applicationArchivesBuildItem, LaunchMode launchMode,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            QuarkusPersistenceUnitDescriptor descriptor) {
        // This defaults to 'import.sql' in non-production modes
        List<String> importFiles = getSqlLoadScript(persistenceUnitConfig.sqlLoadScript(), launchMode);
        if (!importFiles.isEmpty()) {
            List<String> existingImportFiles = new ArrayList<>();
            for (String importFile : importFiles) {
                Path loadScriptPath;
                try {
                    loadScriptPath = applicationArchivesBuildItem.getRootArchive().getChildPath(importFile);
                } catch (RuntimeException e) {
                    throw new ConfigurationException(
                            "Unable to interpret path referenced in '"
                                    + HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "sql-load-script") + "="
                                    + String.join(",", persistenceUnitConfig.sqlLoadScript().get())
                                    + "': " + e.getMessage());
                }

                if (loadScriptPath != null && !Files.isDirectory(loadScriptPath)) {
                    // enlist resource if present
                    existingImportFiles.add(importFile);
                    nativeImageResources.produce(new NativeImageResourceBuildItem(importFile));
                } else if (persistenceUnitConfig.sqlLoadScript().isPresent()) {
                    //raise exception if explicit file is not present (i.e. not the default)
                    throw new ConfigurationException(
                            "Unable to find file referenced in '"
                                    + HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "sql-load-script") + "="
                                    + String.join(",", persistenceUnitConfig.sqlLoadScript().get())
                                    + "'. Remove property or add file to your path.");
                }
                // in dev mode we want to make sure that we watch for changes to file even if it doesn't currently exist
                // as a user could still add it after performing the initial configuration
                hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(importFile));
            }

            if (!existingImportFiles.isEmpty()) {
                descriptor.getProperties().setProperty(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE,
                        String.join(",", existingImportFiles));
            }
        }

        //Disable implicit loading of the default import script (import.sql)
        descriptor.getProperties().setProperty(AvailableSettings.HBM2DDL_SKIP_DEFAULT_IMPORT_FILE, "true");
    }

    public static JsonFormatterCustomizationCheck jsonFormatterCustomizationCheck(Capabilities capabilities,
            Optional<FormatMapperKind> jsonMapper) {
        return jsonMapper.isEmpty() ? JsonFormatterCustomizationCheck.jsonFormatterCustomizationCheckSupplier(false, false)
                : JsonFormatterCustomizationCheck.jsonFormatterCustomizationCheckSupplier(true,
                        capabilities.isPresent(Capability.JACKSON));
    }
}
