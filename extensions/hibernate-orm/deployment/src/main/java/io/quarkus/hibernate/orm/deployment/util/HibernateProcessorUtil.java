package io.quarkus.hibernate.orm.deployment.util;

import static io.quarkus.hibernate.orm.deployment.HibernateConfigUtil.firstPresent;

import java.nio.file.Files;
import java.nio.file.Path;
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
import org.hibernate.loader.BatchFetchStyle;
import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DatabaseKind;
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
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.customized.FormatMapperKind;
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

    public static boolean hasEntities(JpaModelBuildItem jpaModel) {
        return !jpaModel.getEntityClassNames().isEmpty();
    }

    public static Optional<FormatMapperKind> jsonMapperKind(Capabilities capabilities) {
        if (capabilities.isPresent(Capability.JACKSON)) {
            return Optional.of(FormatMapperKind.JACKSON);
        } else if (capabilities.isPresent(Capability.JSONB)) {
            return Optional.of(FormatMapperKind.JSONB);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<FormatMapperKind> xmlMapperKind(Capabilities capabilities) {
        return capabilities.isPresent(Capability.JAXB)
                ? Optional.of(FormatMapperKind.JAXB)
                : Optional.empty();
    }

    public static boolean isHibernateValidatorPresent(Capabilities capabilities) {
        return capabilities.isPresent(Capability.HIBERNATE_VALIDATOR);
    }

    public static void setDialectAndStorageEngine(
            String persistenceUnitName,
            Optional<String> dbKind,
            Optional<String> explicitDialect,
            Optional<String> explicitDbMinVersion,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems,
            Optional<String> storageEngine,
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

        if (storageEngine.isPresent()) {
            if (isMySQLOrMariaDB(dbKind, dialect)) {
                // The storage engine has to be set as a system property.
                // We record it so that we can later run checks (because we can only set a single value)
                storageEngineCollector.add(storageEngine.get());
                systemProperties.produce(new SystemPropertyBuildItem(AvailableSettings.STORAGE_ENGINE, storageEngine.get()));
            } else {
                LOG.warnf(
                        "The storage engine configuration is being ignored because the database is neither MySQL nor MariaDB.");
            }
        }

        if (dbProductVersion.isPresent()) {
            puPropertiesCollector.accept(AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION, dbProductVersion.get());
        }
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

        //charset
        desc.getProperties()
                .setProperty(AvailableSettings.HBM2DDL_CHARSET_NAME, config.database().charset().name());

        // Query
        int batchSize = firstPresent(config.fetch().batchSize(), config.batchFetchSize()).orElse(defaultBatchSize(reactive));
        if (batchSize > 0) {
            desc.getProperties().setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, Integer.toString(batchSize));
            desc.getProperties().setProperty(AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.PADDED.toString());
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
        if (launchMode == LaunchMode.NORMAL) {
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
        // sql-load-scripts
        List<String> importFiles = getSqlLoadScript(persistenceUnitConfig.sqlLoadScript(), launchMode);
        if (!importFiles.isEmpty()) {
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

            // only set the found import files if configured
            if (persistenceUnitConfig.sqlLoadScript().isPresent()) {
                descriptor.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, String.join(",", importFiles));
            }
        } else {
            //Disable implicit loading of the default import script (import.sql)
            descriptor.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, "");
            descriptor.getProperties().setProperty(AvailableSettings.HBM2DDL_SKIP_DEFAULT_IMPORT_FILE, "true");
        }
    }
}
