package io.quarkus.hibernate.reactive.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.loader.BatchFetchStyle;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.hibernate.orm.deployment.HibernateConfigUtil;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor;
import io.quarkus.hibernate.orm.deployment.JpaEntitiesBuildItem;
import io.quarkus.hibernate.orm.deployment.NonJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceProviderSetUpBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.reactive.runtime.FastBootHibernateReactivePersistenceProvider;
import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.hibernate.reactive.runtime.ReactiveSessionFactoryProducer;
import io.quarkus.hibernate.reactive.runtime.ReactiveSessionProducer;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.runtime.LaunchMode;

public final class HibernateReactiveProcessor {

    private static final String HIBERNATE_REACTIVE = "Hibernate Reactive";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.HIBERNATE_REACTIVE);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.HIBERNATE_REACTIVE);
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans, CombinedIndexBuildItem combinedIndex,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }

        if (descriptors.size() == 1) {
            // Only register those beans if their EMF dependency is also available, so use the same guard as the ORM extension
            additionalBeans.produce(new AdditionalBeanBuildItem(ReactiveSessionFactoryProducer.class));
            additionalBeans.produce(new AdditionalBeanBuildItem(ReactiveSessionProducer.class));
        }
    }

    @BuildStep
    void reflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        String[] classes = {
                "org.hibernate.reactive.persister.entity.impl.ReactiveSingleTableEntityPersister",
                "org.hibernate.reactive.persister.collection.impl.ReactiveOneToManyPersister"
        };
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, classes));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(RecorderContext recorderContext,
            HibernateReactiveRecorder recorder,
            JpaEntitiesBuildItem jpaEntities,
            List<NonJpaModelBuildItem> nonJpaModels) {
        final boolean enableRx = hasEntities(jpaEntities, nonJpaModels);
        recorder.callHibernateReactiveFeatureInit(enableRx);
    }

    @BuildStep
    public void buildReactivePersistenceUnit(
            HibernateOrmConfig hibernateOrmConfig,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchModeBuildItem launchMode,
            JpaEntitiesBuildItem domainObjects,
            List<NonJpaModelBuildItem> nonJpaModelBuildItems,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors) {

        final boolean enableHR = hasEntities(domainObjects, nonJpaModelBuildItems);
        if (!enableHR) {
            // we have to bail out early as we might not have a Vertx pool configuration
            return;
        }

        // Block any reactive persistence units from using persistence.xml
        for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptorBuildItem : persistenceXmlDescriptors) {
            String provider = persistenceXmlDescriptorBuildItem.getDescriptor().getProviderClassName();
            if (provider == null ||
                    provider.equals(FastBootHibernateReactivePersistenceProvider.class.getCanonicalName()) ||
                    provider.equals(FastBootHibernateReactivePersistenceProvider.IMPLEMENTATION_NAME)) {
                throw new ConfigurationError(
                        "Cannot use persistence.xml with Hibernate Reactive in Quarkus. Must use application.properties instead.");
            }
        }

        // we only support the default pool for now
        Optional<String> dbKindOptional = dataSourcesBuildTimeConfig.defaultDataSource.dbKind;
        if (dbKindOptional.isPresent()) {
            final String dbKind = dbKindOptional.get();
            ParsedPersistenceXmlDescriptor reactivePU = generateReactivePersistenceUnit(
                    hibernateOrmConfig, domainObjects,
                    dbKind, applicationArchivesBuildItem, launchMode.getLaunchMode(),
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles);

            //Some constant arguments to the following method:
            // - this is Reactive
            // - we don't support starting Hibernate Reactive from a persistence.xml
            // - we don't support Hibernate Envers with Hibernate Reactive
            persistenceUnitDescriptors.produce(new PersistenceUnitDescriptorBuildItem(reactivePU, true, false, false));
        }

    }

    @BuildStep
    @Record(RUNTIME_INIT)
    PersistenceProviderSetUpBuildItem setUpPersistenceProviderAndWaitForVertxPool(
            HibernateReactiveRecorder recorder,
            List<VertxPoolBuildItem> vertxPool,
            HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        recorder.initializePersistenceProvider(hibernateOrmRuntimeConfig);

        // Define a dependency on VertxPoolBuildItem to ensure that any Pool instances are available
        // when HibernateORM starts its persistence units
        runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_REACTIVE));

        return new PersistenceProviderSetUpBuildItem();
    }

    /**
     * This is mostly copied from
     * io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor#handleHibernateORMWithNoPersistenceXml
     * Key differences are:
     * - Always produces a persistence unit descriptor, since we assume there always 1 reactive persistence unit
     * - Any JDBC-only configuration settings are removed
     * - If we ever add any Reactive-only config settings, they can be set here
     */
    private static ParsedPersistenceXmlDescriptor generateReactivePersistenceUnit(
            HibernateOrmConfig hibernateOrmConfig,
            JpaEntitiesBuildItem domainObjects,
            String dbKind,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {

        HibernateOrmConfigPersistenceUnit persistenceUnitConfig = hibernateOrmConfig.defaultPersistenceUnit;

        //we have no persistence.xml so we will create a default one
        Optional<String> dialect = persistenceUnitConfig.dialect.dialect;
        if (!dialect.isPresent()) {
            dialect = HibernateOrmProcessor.guessDialect(dbKind);
        }

        String dialectClassName = dialect.get();
        // we found one
        ParsedPersistenceXmlDescriptor desc = new ParsedPersistenceXmlDescriptor(null); //todo URL
        desc.setName("default-reactive");
        desc.setTransactionType(PersistenceUnitTransactionType.JTA);
        desc.getProperties().setProperty(AvailableSettings.DIALECT, dialectClassName);
        desc.setExcludeUnlistedClasses(true);
        desc.addClasses(new ArrayList<>(domainObjects.getAllModelClassNames()));

        // The storage engine has to be set as a system property.
        if (persistenceUnitConfig.dialect.storageEngine.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(AvailableSettings.STORAGE_ENGINE,
                    persistenceUnitConfig.dialect.storageEngine.get()));
        }
        // Physical Naming Strategy
        persistenceUnitConfig.physicalNamingStrategy.ifPresent(
                namingStrategy -> desc.getProperties()
                        .setProperty(AvailableSettings.PHYSICAL_NAMING_STRATEGY, namingStrategy));

        // Implicit Naming Strategy
        persistenceUnitConfig.implicitNamingStrategy.ifPresent(
                namingStrategy -> desc.getProperties()
                        .setProperty(AvailableSettings.IMPLICIT_NAMING_STRATEGY, namingStrategy));

        //charset
        desc.getProperties().setProperty(AvailableSettings.HBM2DDL_CHARSET_NAME,
                persistenceUnitConfig.database.charset.name());

        persistenceUnitConfig.database.defaultCatalog.ifPresent(
                catalog -> desc.getProperties().setProperty(AvailableSettings.DEFAULT_CATALOG, catalog));

        persistenceUnitConfig.database.defaultSchema.ifPresent(
                schema -> desc.getProperties().setProperty(AvailableSettings.DEFAULT_SCHEMA, schema));

        if (persistenceUnitConfig.database.globallyQuotedIdentifiers) {
            desc.getProperties().setProperty(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, "true");
        }

        // Query
        if (persistenceUnitConfig.batchFetchSize > 0) {
            desc.getProperties().setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE,
                    Integer.toString(persistenceUnitConfig.batchFetchSize));
            desc.getProperties().setProperty(AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.PADDED.toString());
        }

        persistenceUnitConfig.maxFetchDepth.ifPresent(
                depth -> desc.getProperties().setProperty(AvailableSettings.MAX_FETCH_DEPTH, String.valueOf(depth)));

        desc.getProperties().setProperty(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, Integer.toString(
                persistenceUnitConfig.query.queryPlanCacheMaxSize));

        desc.getProperties().setProperty(AvailableSettings.DEFAULT_NULL_ORDERING,
                persistenceUnitConfig.query.defaultNullOrdering.name().toLowerCase());

        // Statistics
        if (hibernateOrmConfig.metricsEnabled
                || (hibernateOrmConfig.statistics.isPresent() && hibernateOrmConfig.statistics.get())) {
            desc.getProperties().setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
        }

        // sql-load-script
        Optional<String> importFile = getSqlLoadScript(persistenceUnitConfig.sqlLoadScript, launchMode);

        if (importFile.isPresent()) {
            Path loadScriptPath = applicationArchivesBuildItem.getRootArchive().getChildPath(importFile.get());

            if (loadScriptPath != null && !Files.isDirectory(loadScriptPath)) {
                // enlist resource if present
                nativeImageResources.produce(new NativeImageResourceBuildItem(importFile.get()));
                hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(importFile.get()));
                desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, importFile.get());
            } else if (persistenceUnitConfig.sqlLoadScript.isPresent()) {
                //raise exception if explicit file is not present (i.e. not the default)
                throw new ConfigurationError(
                        "Unable to find file referenced in '" + HibernateOrmProcessor.HIBERNATE_ORM_CONFIG_PREFIX
                                + "sql-load-script="
                                + persistenceUnitConfig.sqlLoadScript.get() + "'. Remove property or add file to your path.");
            }
        } else {
            //Disable implicit loading of the default import script (import.sql)
            desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, "");
        }

        // Caching
        if (persistenceUnitConfig.secondLevelCachingEnabled) {
            Properties p = desc.getProperties();
            //Only set these if the user isn't making an explicit choice:
            p.putIfAbsent(USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.TRUE);
            p.putIfAbsent(USE_SECOND_LEVEL_CACHE, Boolean.TRUE);
            p.putIfAbsent(USE_QUERY_CACHE, Boolean.TRUE);
            p.putIfAbsent(JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE);
            Map<String, String> cacheConfigEntries = HibernateConfigUtil.getCacheConfigEntries(persistenceUnitConfig);
            for (Entry<String, String> entry : cacheConfigEntries.entrySet()) {
                desc.getProperties().setProperty(entry.getKey(), entry.getValue());
            }
        } else {
            //Unless the global switch is explicitly set to off, in which case we disable all caching:
            Properties p = desc.getProperties();
            p.put(USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.FALSE);
            p.put(USE_SECOND_LEVEL_CACHE, Boolean.FALSE);
            p.put(USE_QUERY_CACHE, Boolean.FALSE);
            p.put(JPA_SHARED_CACHE_MODE, SharedCacheMode.NONE);
        }

        return desc;
    }

    private static Optional<String> getSqlLoadScript(Optional<String> sqlLoadScript, LaunchMode launchMode) {
        // Explicit file or default Hibernate ORM file.
        if (sqlLoadScript.isPresent()) {
            if (HibernateOrmProcessor.NO_SQL_LOAD_SCRIPT_FILE.equalsIgnoreCase(sqlLoadScript.get())) {
                return Optional.empty();
            } else {
                return Optional.of(sqlLoadScript.get());
            }
        } else if (launchMode == LaunchMode.NORMAL) {
            return Optional.empty();
        } else {
            return Optional.of("import.sql");
        }
    }

    private boolean hasEntities(JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        return !jpaEntities.getEntityClassNames().isEmpty() || !nonJpaModels.isEmpty();
    }

}
