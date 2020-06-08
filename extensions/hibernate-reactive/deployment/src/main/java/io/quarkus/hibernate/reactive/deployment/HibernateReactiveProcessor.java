package io.quarkus.hibernate.reactive.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;

import java.nio.file.Files;
import java.nio.file.Path;
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
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.hibernate.orm.deployment.HibernateConfigUtil;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor;
import io.quarkus.hibernate.orm.deployment.JpaEntitiesBuildItem;
import io.quarkus.hibernate.orm.deployment.NonJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.reactive.runtime.FastBootHibernateReactivePersistenceProvider;
import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.hibernate.reactive.runtime.ReactiveSessionFactoryProducer;
import io.quarkus.hibernate.reactive.runtime.ReactiveSessionProducer;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.runtime.LaunchMode;

public final class HibernateReactiveProcessor {

    private static final String HIBERNATE_REACTIVE = "Hibernate Reactive";

    /**
     * Hibernate ORM configuration
     */
    HibernateOrmConfig hibernateConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.HIBERNATE_REACTIVE);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.HIBERNATE_REACTIVE);
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ReactiveSessionFactoryProducer.class)
                .addBeanClass(ReactiveSessionProducer.class)
                .build());
    }

    @BuildStep
    void reflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        String[] classes = {
                "org.hibernate.reactive.persister.entity.impl.ReactiveSingleTableEntityPersister"
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
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchModeBuildItem launchMode,
            JpaEntitiesBuildItem domainObjects,
            List<NonJpaModelBuildItem> nonJpaModelBuildItems,
            BuildProducer<SystemPropertyBuildItem> systemPropertyProducer,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorProducer) {

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
        Optional<String> dbKind = dataSourcesBuildTimeConfig.defaultDataSource.dbKind;

        ParsedPersistenceXmlDescriptor reactivePU = generateReactivePersistenceUnit(resourceProducer, systemPropertyProducer,
                dbKind, applicationArchivesBuildItem, launchMode.getLaunchMode());

        persistenceUnitDescriptorProducer.produce(new PersistenceUnitDescriptorBuildItem(reactivePU));
    }

    private ParsedPersistenceXmlDescriptor generateReactivePersistenceUnit(
            //            List<ParsedPersistenceXmlDescriptor> descriptors,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            Optional<String> dbKind,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode) {

        //we have no persistence.xml so we will create a default one
        Optional<String> dialect = hibernateConfig.dialect;
        if (!dialect.isPresent()) {
            dialect = HibernateOrmProcessor.guessDialect(dbKind);
        }

        String dialectClassName = dialect.get();
        // we found one
        ParsedPersistenceXmlDescriptor desc = new ParsedPersistenceXmlDescriptor(null); //todo URL
        desc.setName("default-reactive");
        desc.setTransactionType(PersistenceUnitTransactionType.JTA);
        desc.getProperties().setProperty(AvailableSettings.DIALECT, dialectClassName);

        // The storage engine has to be set as a system property.
        if (hibernateConfig.dialectStorageEngine.isPresent()) {
            systemProperty.produce(new SystemPropertyBuildItem(AvailableSettings.STORAGE_ENGINE,
                    hibernateConfig.dialectStorageEngine.get()));
        }
        // Physical Naming Strategy
        hibernateConfig.physicalNamingStrategy.ifPresent(
                namingStrategy -> desc.getProperties()
                        .setProperty(AvailableSettings.PHYSICAL_NAMING_STRATEGY, namingStrategy));

        // Implicit Naming Strategy
        hibernateConfig.implicitNamingStrategy.ifPresent(
                namingStrategy -> desc.getProperties()
                        .setProperty(AvailableSettings.IMPLICIT_NAMING_STRATEGY, namingStrategy));

        // Database
        desc.getProperties().setProperty(AvailableSettings.HBM2DDL_DATABASE_ACTION,
                hibernateConfig.database.generation);

        if (hibernateConfig.database.generationHaltOnError) {
            desc.getProperties().setProperty(AvailableSettings.HBM2DDL_HALT_ON_ERROR, "true");
        }

        //charset
        desc.getProperties().setProperty(AvailableSettings.HBM2DDL_CHARSET_NAME,
                hibernateConfig.database.charset.name());

        hibernateConfig.database.defaultCatalog.ifPresent(
                catalog -> desc.getProperties().setProperty(AvailableSettings.DEFAULT_CATALOG, catalog));

        hibernateConfig.database.defaultSchema.ifPresent(
                schema -> desc.getProperties().setProperty(AvailableSettings.DEFAULT_SCHEMA, schema));

        if (hibernateConfig.database.globallyQuotedIdentifiers) {
            desc.getProperties().setProperty(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, "true");
        }

        // Query
        if (hibernateConfig.batchFetchSize > 0) {
            desc.getProperties().setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE,
                    Integer.toString(hibernateConfig.batchFetchSize));
            desc.getProperties().setProperty(AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.PADDED.toString());
        }

        hibernateConfig.query.queryPlanCacheMaxSize.ifPresent(
                maxSize -> desc.getProperties().setProperty(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, maxSize));

        hibernateConfig.query.defaultNullOrdering.ifPresent(
                defaultNullOrdering -> desc.getProperties().setProperty(AvailableSettings.DEFAULT_NULL_ORDERING,
                        defaultNullOrdering));

        // Logging
        if (hibernateConfig.log.sql) {
            desc.getProperties().setProperty(AvailableSettings.SHOW_SQL, "true");
            desc.getProperties().setProperty(AvailableSettings.FORMAT_SQL, "true");
        }

        // Statistics
        if (hibernateConfig.metricsEnabled
                || (hibernateConfig.statistics.isPresent() && hibernateConfig.statistics.get())) {
            desc.getProperties().setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
        }

        // sql-load-script
        Optional<String> importFile = getSqlLoadScript(launchMode);

        if (!importFile.isPresent()) {
            // explicitly set a no file and ignore all other operations
            desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES,
                    HibernateOrmProcessor.NO_SQL_LOAD_SCRIPT_FILE);
        } else {
            Path loadScriptPath = applicationArchivesBuildItem.getRootArchive().getChildPath(importFile.get());

            if (loadScriptPath != null && !Files.isDirectory(loadScriptPath)) {
                // enlist resource if present
                resourceProducer.produce(new NativeImageResourceBuildItem(importFile.get()));
                desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, importFile.get());
                desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR,
                        MultipleLinesSqlCommandExtractor.class.getName());

            } else if (hibernateConfig.sqlLoadScript.isPresent()) {
                //raise exception if explicit file is not present (i.e. not the default)
                throw new ConfigurationError(
                        "Unable to find file referenced in '" + HibernateOrmProcessor.HIBERNATE_ORM_CONFIG_PREFIX
                                + "sql-load-script="
                                + hibernateConfig.sqlLoadScript.get() + "'. Remove property or add file to your path.");
            }
        }

        // Caching
        if (hibernateConfig.secondLevelCachingEnabled) {
            Properties p = desc.getProperties();
            //Only set these if the user isn't making an explicit choice:
            p.putIfAbsent(USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.TRUE);
            p.putIfAbsent(USE_SECOND_LEVEL_CACHE, Boolean.TRUE);
            p.putIfAbsent(USE_QUERY_CACHE, Boolean.TRUE);
            p.putIfAbsent(JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE);
            Map<String, String> cacheConfigEntries = HibernateConfigUtil.getCacheConfigEntries(hibernateConfig);
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

    private Optional<String> getSqlLoadScript(LaunchMode launchMode) {
        // Explicit file or default Hibernate ORM file.
        if (hibernateConfig.sqlLoadScript.isPresent()) {
            if (HibernateOrmProcessor.NO_SQL_LOAD_SCRIPT_FILE.equalsIgnoreCase(hibernateConfig.sqlLoadScript.get())) {
                return Optional.empty();
            } else {
                return Optional.of(hibernateConfig.sqlLoadScript.get());
            }
        } else if (launchMode == LaunchMode.NORMAL) {
            return Optional.empty();
        } else {
            return Optional.of("import.sql");
        }
    }

    @BuildStep
    void waitForVertxPool(
            List<VertxPoolBuildItem> vertxPool,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        // Define a dependency on VertxPoolBuildItem to ensure that any Pool instances are available
        // when HibernateORM starts its persistence units
        runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_REACTIVE));
    }

    private boolean hasEntities(JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        return !jpaEntities.getEntityClassNames().isEmpty() || !nonJpaModels.isEmpty();
    }

}
